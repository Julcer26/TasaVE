package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class TasaVeUiState(
    val isLoading: Boolean = false,
    val usdBcv: RateInfo? = null,
    val eurBcv: RateInfo? = null,
    val usdtParalelo: RateInfo? = null,
    val isFromCache: Boolean = false,
    val isOfflineError: Boolean = false,
    val errorMessage: String? = null
)

class TasaVeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database by lazy { AppDatabase.getDatabase(application) }
    private val repository by lazy { TasaVeRepository(application, database.conversionHistoryDao) }

    private val _uiState = MutableStateFlow(TasaVeUiState())
    val uiState: StateFlow<TasaVeUiState> = _uiState.asStateFlow()

    private val _themeMode = MutableStateFlow("DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Selection & Dynamic Calculator state
    var selectedRateKey by mutableStateOf("USD_BCV")
        private set

    var isVesAsInput by mutableStateOf(false)
        private set

    var foreignAmount by mutableStateOf("")
        private set

    var vesAmount by mutableStateOf("")
        private set

    // Decimal formatter for standard clean presentation
    private val decimalFormatter = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    init {
        // Load initial theme from localStorage/SharedPreferences
        _themeMode.value = repository.getLocalTheme()
        
        // Load cached/fallback rates immediately so user sees numbers and can calculate instantly without network delays!
        val cached = repository.getCachedRates()
        _uiState.value = TasaVeUiState(
            isLoading = false,
            usdBcv = cached.first,
            eurBcv = cached.second,
            usdtParalelo = cached.third,
            isFromCache = true
        )
        recalculateVES()

        // Fetch fresh rates asynchronously
        fetchRates()
    }

    val historyState: StateFlow<List<ConversionHistory>> = repository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleTheme() {
        val nextTheme = if (_themeMode.value == "DARK") "LIGHT" else "DARK"
        _themeMode.value = nextTheme
        repository.saveLocalTheme(nextTheme)
    }

    fun fetchRates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.fetchRates(forceRefresh)) {
                is TasaVeRatesResult.Success -> {
                    _uiState.value = TasaVeUiState(
                        isLoading = false,
                        usdBcv = result.usdBcv,
                        eurBcv = result.eurBcv,
                        usdtParalelo = result.usdtParalelo,
                        isFromCache = result.isFromCache,
                        isOfflineError = result.isOfflineError
                    )
                    // Reset inputs or recalculate with new rates
                    recalculateVES()
                }
                is TasaVeRatesResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun selectRate(key: String) {
        selectedRateKey = key
        // Keep inputs as they are, but recalculate the alternate amount based on the new rate
        recalculateVES()
    }

    fun updateForeignAmount(input: String) {
        if (input.isEmpty()) {
            foreignAmount = ""
            vesAmount = ""
            return
        }

        // Keep input raw to prevent keyboard/IME desync, but filter invalid characters and multiple decimal separators
        val dotCount = input.count { it == '.' }
        val commaCount = input.count { it == ',' }
        if (dotCount + commaCount <= 1 && input.all { it.isDigit() || it == '.' || it == ',' }) {
            foreignAmount = input

            val sanitized = input.replace(",", ".")
            val value = sanitized.toDoubleOrNull() ?: 0.0
            val rate = getSelectedRateValue()
            vesAmount = if (rate > 0) {
                val calculated = value * rate
                // Present in clean string format
                String.format(Locale.US, "%.2f", calculated)
            } else {
                ""
            }
        }
    }

    fun updateVesAmount(input: String) {
        if (input.isEmpty()) {
            vesAmount = ""
            foreignAmount = ""
            return
        }

        // Keep input raw to prevent keyboard/IME desync, but filter invalid characters and multiple decimal separators
        val dotCount = input.count { it == '.' }
        val commaCount = input.count { it == ',' }
        if (dotCount + commaCount <= 1 && input.all { it.isDigit() || it == '.' || it == ',' }) {
            vesAmount = input

            val sanitized = input.replace(",", ".")
            val value = sanitized.toDoubleOrNull() ?: 0.0
            val rate = getSelectedRateValue()
            foreignAmount = if (rate > 0) {
                val calculated = value / rate
                String.format(Locale.US, "%.2f", calculated)
            } else {
                ""
            }
        }
    }

    fun swapAmounts() {
        isVesAsInput = !isVesAsInput
        val rate = getSelectedRateValue()
        if (rate <= 0) return

        if (isVesAsInput) {
            // We transitioned from (foreign as input) to (ves as input).
            // Retain calculated/entered vesAmount, and update foreignAmount.
            val value = parseDouble(vesAmount)
            if (value != null) {
                foreignAmount = String.format(Locale.US, "%.2f", value / rate)
            } else {
                foreignAmount = ""
            }
        } else {
            // We transitioned from (ves as input) to (foreign as input).
            // Retain calculated/entered foreignAmount, and update vesAmount.
            val value = parseDouble(foreignAmount)
            if (value != null) {
                vesAmount = String.format(Locale.US, "%.2f", value * rate)
            } else {
                vesAmount = ""
            }
        }
    }

    fun saveConversionToHistory() {
        val fVal = parseDouble(foreignAmount) ?: return
        val vVal = parseDouble(vesAmount) ?: return
        val rate = getSelectedRateValue()
        if (rate <= 0.0) return

        val rateObj = getSelectedRateInfo() ?: return

        viewModelScope.launch {
            repository.insertHistory(
                ConversionHistory(
                    amountFrom = fVal,
                    currencyFrom = rateObj.symbol,
                    amountTo = vVal,
                    currencyTo = "Bs.",
                    rate = rate
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun getSelectedRateValue(): Double {
        return getSelectedRateInfo()?.rate ?: 0.0
    }

    fun getSelectedRateInfo(): RateInfo? {
        val state = _uiState.value
        return when (selectedRateKey) {
            "USD_BCV" -> state.usdBcv
            "EUR_BCV" -> state.eurBcv
            "USDT" -> state.usdtParalelo
            else -> null
        }
    }

    private fun parseDouble(valueStr: String): Double? {
        return valueStr.replace(",", ".").toDoubleOrNull()
    }

    private fun recalculateVES() {
        if (isVesAsInput) {
            val vVal = parseDouble(vesAmount)
            if (vVal != null) {
                val rate = getSelectedRateValue()
                if (rate > 0) {
                    val calculated = vVal / rate
                    foreignAmount = String.format(Locale.US, "%.2f", calculated)
                }
            } else {
                foreignAmount = ""
            }
        } else {
            val fVal = parseDouble(foreignAmount)
            if (fVal != null) {
                val rate = getSelectedRateValue()
                if (rate > 0) {
                    val calculated = fVal * rate
                    vesAmount = String.format(Locale.US, "%.2f", calculated)
                }
            } else {
                vesAmount = ""
            }
        }
    }
}
