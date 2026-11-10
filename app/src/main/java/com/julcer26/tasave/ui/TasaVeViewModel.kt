package com.julcer26.tasave.ui

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.julcer26.tasave.billing.BillingManager
import com.julcer26.tasave.data.*
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
    private val localStorageHelper by lazy { LocalStorageHelper(application) }

    // Official Google Play Billing integration
    val billingManager by lazy {
        BillingManager(
            context = application,
            storageHelper = localStorageHelper,
            scope = viewModelScope
        )
    }

    val isProUser: StateFlow<Boolean> = billingManager.isProUser
    val proProductPrice: StateFlow<String> = billingManager.productPrice
    val purchaseStatusMessage: StateFlow<String?> = billingManager.purchaseStatusMessage

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

    fun purchasePro(activity: Activity) {
        billingManager.launchPurchase(activity)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    fun clearPurchaseStatus() {
        billingManager.clearStatusMessage()
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
                    // Recalculate with newly fetched rates
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

        val dotCount = input.count { it == '.' }
        val commaCount = input.count { it == ',' }
        if (dotCount + commaCount <= 1 && input.all { it.isDigit() || it == '.' || it == ',' }) {
            foreignAmount = input

            val sanitized = input.replace(",", ".")
            val value = sanitized.toDoubleOrNull() ?: 0.0
            val rate = getSelectedRateValue()
            vesAmount = if (rate > 0) {
                val calculated = value * rate
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
            val value = parseDouble(vesAmount)
            foreignAmount = if (value != null) {
                String.format(Locale.US, "%.2f", value / rate)
            } else {
                ""
            }
        } else {
            val value = parseDouble(foreignAmount)
            vesAmount = if (value != null) {
                String.format(Locale.US, "%.2f", value * rate)
            } else {
                ""
            }
        }
    }

    fun saveConversionToHistory(): Boolean {
        val fVal = parseDouble(foreignAmount) ?: return false
        val vVal = parseDouble(vesAmount) ?: return false
        val rate = getSelectedRateValue()
        if (rate <= 0.0) return false

        val rateObj = getSelectedRateInfo() ?: return false

        val (fromAmount, fromCurrency, toAmount, toCurrency) = if (isVesAsInput) {
            listOf(vVal, "Bs.", fVal, rateObj.symbol)
        } else {
            listOf(fVal, rateObj.symbol, vVal, "Bs.")
        }

        viewModelScope.launch {
            repository.insertHistory(
                ConversionHistory(
                    amountFrom = fromAmount as Double,
                    currencyFrom = fromCurrency as String,
                    amountTo = toAmount as Double,
                    currencyTo = toCurrency as String,
                    rate = rate
                )
            )
        }
        return true
    }

    fun loadConversion(item: ConversionHistory) {
        val matchingRate = when (item.currencyFrom) {
            "$" -> "USD_BCV"
            "€" -> "EUR_BCV"
            "₮" -> "USDT"
            else -> when (item.currencyTo) {
                "$" -> "USD_BCV"
                "€" -> "EUR_BCV"
                "₮" -> "USDT"
                else -> null
            }
        }
        if (matchingRate != null) {
            selectedRateKey = matchingRate
        }

        if (item.currencyFrom == "Bs.") {
            isVesAsInput = true
            vesAmount = String.format(Locale.US, "%.2f", item.amountFrom)
            foreignAmount = String.format(Locale.US, "%.2f", item.amountTo)
        } else {
            isVesAsInput = false
            foreignAmount = String.format(Locale.US, "%.2f", item.amountFrom)
            vesAmount = String.format(Locale.US, "%.2f", item.amountTo)
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

    override fun onCleared() {
        super.onCleared()
        billingManager.destroy()
    }
}
