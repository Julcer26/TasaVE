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
import com.julcer26.tasave.ultra.*
import com.julcer26.tasave.widget.TasaVeRatesWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    // Official Google Play Billing integration (Pro + Ultra)
    val billingManager by lazy {
        BillingManager(
            context = application,
            storageHelper = localStorageHelper,
            scope = viewModelScope
        )
    }

    val isProUser: StateFlow<Boolean> = billingManager.isProUser
    val isUltraUser: StateFlow<Boolean> = billingManager.isUltraUser
    val debugTier: StateFlow<UserTier> = billingManager.debugTier
    val proProductPrice: StateFlow<String> = billingManager.productPrice
    val ultraMonthlyPrice: StateFlow<String> = billingManager.ultraMonthlyPrice
    val ultraYearlyPrice: StateFlow<String> = billingManager.ultraYearlyPrice
    val purchaseStatusMessage: StateFlow<String?> = billingManager.purchaseStatusMessage

    private val _uiState = MutableStateFlow(TasaVeUiState())
    val uiState: StateFlow<TasaVeUiState> = _uiState.asStateFlow()

    private val _themeMode = MutableStateFlow("DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _remainingFreeScans = MutableStateFlow(localStorageHelper.getRemainingFreeScans())
    val remainingFreeScans: StateFlow<Int> = _remainingFreeScans.asStateFlow()

    // Ultra: Shopping Cart State
    private val _cartItems = MutableStateFlow(localStorageHelper.getCartItems())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _cartIvaEnabled = MutableStateFlow(localStorageHelper.isCartIvaEnabled())
    val cartIvaEnabled: StateFlow<Boolean> = _cartIvaEnabled.asStateFlow()

    private val _cartIgtfEnabled = MutableStateFlow(localStorageHelper.isCartIgtfEnabled())
    val cartIgtfEnabled: StateFlow<Boolean> = _cartIgtfEnabled.asStateFlow()

    // Ultra: Market / Brecha Snapshots & Alerts
    private val _rateSnapshots = MutableStateFlow(localStorageHelper.getRateSnapshots())
    val rateSnapshots: StateFlow<List<RateSnapshot>> = _rateSnapshots.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private val _alertSettings = MutableStateFlow(localStorageHelper.getAlertSettings())
    val alertSettings: StateFlow<AlertSettings> = _alertSettings.asStateFlow()

    // Ultra: Checkout / Mixed Payment & Multiple Payment Profiles (Pago Móvil + Bank Accounts)
    private val _pagoMovilInfo = MutableStateFlow(localStorageHelper.getPagoMovilInfo())
    val pagoMovilInfo: StateFlow<PagoMovilInfo> = _pagoMovilInfo.asStateFlow()

    private val _paymentProfiles = MutableStateFlow(localStorageHelper.getPaymentProfiles())
    val paymentProfiles: StateFlow<List<PaymentMethodProfile>> = _paymentProfiles.asStateFlow()

    private val _selectedPaymentProfileId = MutableStateFlow(
        localStorageHelper.getSelectedPaymentProfileId()
            ?: _paymentProfiles.value.firstOrNull()?.id
    )
    val selectedPaymentProfileId: StateFlow<Long?> = _selectedPaymentProfileId.asStateFlow()

    // Selection & Dynamic Calculator state
    var selectedRateKey by mutableStateOf("USD_BCV")
        private set

    var isVesAsInput by mutableStateOf(false)
        private set

    var foreignAmount by mutableStateOf("")
        private set

    var vesAmount by mutableStateOf("")
        private set

    init {
        _themeMode.value = repository.getLocalTheme()

        val cached = repository.getCachedRates()
        _uiState.value = TasaVeUiState(
            isLoading = false,
            usdBcv = cached.first,
            eurBcv = cached.second,
            usdtParalelo = cached.third,
            isFromCache = true
        )
        recalculateVES()
        fetchRates()
        fetchHistoricalRates()
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

    fun setDebugTier(tier: UserTier) {
        billingManager.setDebugTier(tier)
    }

    fun purchasePro(activity: Activity) {
        billingManager.launchPurchase(activity)
    }

    fun purchaseUltra(activity: Activity, yearly: Boolean = false) {
        billingManager.launchUltraSubscription(activity, yearly)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    fun clearPurchaseStatus() {
        billingManager.clearStatusMessage()
    }

    fun fetchRates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
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
                    recalculateVES()
                    TasaVeRatesWidgetProvider.updateAllWidgets(getApplication())
                    fetchHistoricalRates(forceRefresh = forceRefresh)
                }
                is TasaVeRatesResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun fetchHistoricalRates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isHistoryLoading.value = true
            val merged = repository.fetchHistoricalRates(forceRefresh)
            if (merged.isNotEmpty()) {
                _rateSnapshots.value = merged
            } else {
                _rateSnapshots.value = localStorageHelper.getRateSnapshots()
            }
            _isHistoryLoading.value = false
        }
    }

    fun selectRate(key: String) {
        selectedRateKey = key
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

    fun refreshRemainingFreeScans() {
        _remainingFreeScans.value = localStorageHelper.getRemainingFreeScans()
    }

    fun applyScannedPrice(amount: Double, isVes: Boolean, targetRateKey: String? = null) {
        if (targetRateKey != null) {
            selectedRateKey = targetRateKey
        }
        isVesAsInput = isVes
        val formattedInput = String.format(Locale.US, "%.2f", amount)
        if (isVes) {
            updateVesAmount(formattedInput)
        } else {
            updateForeignAmount(formattedInput)
        }
        if (!isProUser.value) {
            _remainingFreeScans.value = localStorageHelper.recordFreeScanUsed()
        }
    }

    // --- Ultra: Shopping Cart Actions ---
    fun addCartItem(name: String, amount: Double, isVes: Boolean, rateKey: String = "USD_BCV", quantity: Int = 1) {
        val rate = when (rateKey) {
            "EUR_BCV" -> _uiState.value.eurBcv?.rate
            "USDT" -> _uiState.value.usdtParalelo?.rate
            else -> _uiState.value.usdBcv?.rate
        } ?: getSelectedRateValue().takeIf { it > 0 } ?: 1.0

        val usdBcvRate = _uiState.value.usdBcv?.rate ?: rate
        val unitUsd = if (isVes) {
            if (usdBcvRate > 0) amount / usdBcvRate else 0.0
        } else {
            if (rateKey == "USD_BCV" || usdBcvRate <= 0.0) amount else (amount * rate) / usdBcvRate
        }
        if (unitUsd <= 0.0) return

        val nextList = _cartItems.value.toMutableList().apply {
            add(
                0,
                CartItem(
                    id = System.currentTimeMillis(),
                    name = name.ifBlank { "Producto #${size + 1}" },
                    unitPriceUsd = unitUsd,
                    quantity = quantity.coerceAtLeast(1)
                )
            )
        }
        _cartItems.value = nextList
        localStorageHelper.saveCartItems(nextList)
    }

    fun editCartItem(id: Long, name: String, amount: Double, isVes: Boolean, quantity: Int) {
        val usdBcvRate = _uiState.value.usdBcv?.rate ?: getSelectedRateValue().takeIf { it > 0 } ?: 1.0
        val unitUsd = if (isVes) {
            if (usdBcvRate > 0) amount / usdBcvRate else 0.0
        } else {
            amount
        }
        if (unitUsd <= 0.0) return

        val nextList = _cartItems.value.map { item ->
            if (item.id == id) {
                item.copy(
                    name = name.ifBlank { item.name },
                    unitPriceUsd = unitUsd,
                    quantity = quantity.coerceAtLeast(1)
                )
            } else {
                item
            }
        }
        _cartItems.value = nextList
        localStorageHelper.saveCartItems(nextList)
    }

    fun updateCartItemQuantity(id: Long, delta: Int) {
        val nextList = _cartItems.value.mapNotNull { item ->
            if (item.id != id) {
                item
            } else {
                val nextQty = item.quantity + delta
                if (nextQty <= 0) null else item.copy(quantity = nextQty)
            }
        }
        _cartItems.value = nextList
        localStorageHelper.saveCartItems(nextList)
    }

    fun removeCartItem(id: Long) {
        val nextList = _cartItems.value.filterNot { it.id == id }
        _cartItems.value = nextList
        localStorageHelper.saveCartItems(nextList)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        localStorageHelper.saveCartItems(emptyList())
    }

    fun toggleCartIva() {
        val next = !_cartIvaEnabled.value
        _cartIvaEnabled.value = next
        localStorageHelper.setCartIvaEnabled(next)
    }

    fun toggleCartIgtf() {
        val next = !_cartIgtfEnabled.value
        _cartIgtfEnabled.value = next
        localStorageHelper.setCartIgtfEnabled(next)
    }

    // --- Ultra: Alerts & Widget ---
    fun updateAlertSettings(updated: AlertSettings) {
        _alertSettings.value = updated
        localStorageHelper.saveAlertSettings(updated)
        if (updated.enabled) {
            val state = _uiState.value
            localStorageHelper.saveLastNotifiedRates(
                state.usdBcv?.rate ?: 0.0,
                state.eurBcv?.rate ?: 0.0,
                state.usdtParalelo?.rate ?: 0.0
            )
        }
        RateAlertWorker.scheduleOrCancel(getApplication(), updated.enabled)
    }

    fun triggerTestAlertNotification(): Boolean {
        val state = _uiState.value
        val usd = state.usdBcv?.rate ?: 0.0
        val eur = state.eurBcv?.rate ?: 0.0
        val usdt = state.usdtParalelo?.rate ?: 0.0
        return RateAlertWorker.sendAlertNotification(
            context = getApplication(),
            title = "Alerta Tasa VE Ultra",
            body = "USD BCV: Bs. ${String.format(Locale.US, "%.2f", usd)} • EUR: Bs. ${String.format(Locale.US, "%.2f", eur)} • USDT: Bs. ${String.format(Locale.US, "%.2f", usdt)}"
        )
    }

    // --- Ultra: Payment Profiles (Multiple Pago Móvil & Bank Accounts) ---
    fun savePaymentProfile(profile: PaymentMethodProfile) {
        val current = _paymentProfiles.value.toMutableList()
        val finalProfile = if (profile.id <= 0L) {
            profile.copy(id = System.currentTimeMillis())
        } else {
            profile
        }
        val idx = current.indexOfFirst { it.id == finalProfile.id }
        if (idx >= 0) {
            current[idx] = finalProfile
        } else {
            current.add(0, finalProfile)
        }
        _paymentProfiles.value = current
        localStorageHelper.savePaymentProfiles(current)
        selectPaymentProfile(finalProfile.id)
    }

    fun deletePaymentProfile(id: Long) {
        val next = _paymentProfiles.value.filterNot { it.id == id }
        _paymentProfiles.value = next
        localStorageHelper.savePaymentProfiles(next)
        if (_selectedPaymentProfileId.value == id) {
            selectPaymentProfile(next.firstOrNull()?.id)
        }
    }

    fun selectPaymentProfile(id: Long?) {
        _selectedPaymentProfileId.value = id
        localStorageHelper.setSelectedPaymentProfileId(id)
    }

    fun savePagoMovilInfo(info: PagoMovilInfo) {
        _pagoMovilInfo.value = info
        localStorageHelper.savePagoMovilInfo(info)
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
