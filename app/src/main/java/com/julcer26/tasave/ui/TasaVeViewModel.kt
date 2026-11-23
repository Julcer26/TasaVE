package com.julcer26.tasave.ui

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.julcer26.tasave.analytics.AnalyticsTracker
import com.julcer26.tasave.billing.BillingManager
import com.julcer26.tasave.data.*
import com.julcer26.tasave.quick.PersistentRateNotificationManager
import com.julcer26.tasave.share.ShareCardGenerator
import com.julcer26.tasave.ultra.*
import com.julcer26.tasave.voice.ParsedVoiceConversion
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
    val nextUsdBcv: RateInfo? = null,
    val nextEurBcv: RateInfo? = null,
    val nextBcvDateIso: String? = null,
    val copPerUsd: Double = 4320.0,
    val copRate: RateInfo? = null,
    val isFromCache: Boolean = false,
    val isOfflineError: Boolean = false,
    val errorMessage: String? = null
)

class TasaVeViewModel(application: Application) : AndroidViewModel(application) {

    private val database by lazy { AppDatabase.getDatabase(application) }
    private val repository by lazy { TasaVeRepository(application, database.conversionHistoryDao) }
    private val localStorageHelper by lazy { LocalStorageHelper(application) }

    // Official Google Play Billing integration (Ultra)
    val billingManager by lazy {
        BillingManager(
            context = application,
            storageHelper = localStorageHelper,
            scope = viewModelScope
        )
    }

    val isUltraUser: StateFlow<Boolean> = billingManager.isUltraUser
    val debugTier: StateFlow<UserTier> = billingManager.debugTier
    val ultraMonthlyPrice: StateFlow<String> = billingManager.ultraMonthlyPrice
    val ultraYearlyPrice: StateFlow<String> = billingManager.ultraYearlyPrice
    val purchaseStatusMessage: StateFlow<String?> = billingManager.purchaseStatusMessage

    private val _uiState = MutableStateFlow(TasaVeUiState())
    val uiState: StateFlow<TasaVeUiState> = _uiState.asStateFlow()

    private val _themeMode = MutableStateFlow("DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _remainingFreeScans = MutableStateFlow(localStorageHelper.getRemainingFreeScans())
    val remainingFreeScans: StateFlow<Int> = _remainingFreeScans.asStateFlow()

    private val _remainingFreeVoiceUses = MutableStateFlow(localStorageHelper.getRemainingFreeVoiceUses())
    val remainingFreeVoiceUses: StateFlow<Int> = _remainingFreeVoiceUses.asStateFlow()

    private val _voiceTtsEnabled = MutableStateFlow(localStorageHelper.isVoiceTtsEnabled())
    val voiceTtsEnabled: StateFlow<Boolean> = _voiceTtsEnabled.asStateFlow()

    val appLaunchCount: Int = localStorageHelper.incrementAndGetAppLaunchCount()

    private val _adFreePassUntilMs = MutableStateFlow(localStorageHelper.getAdFreePassUntilMs())
    val adFreePassUntilMs: StateFlow<Long> = _adFreePassUntilMs.asStateFlow()

    fun grantAdFreePass12Hours(): Long {
        val until = localStorageHelper.grantAdFreePassHours(12)
        _adFreePassUntilMs.value = until
        return until
    }

    // Shopping Cart State
    private val _cartItems = MutableStateFlow(localStorageHelper.getCartItems())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _cartIvaEnabled = MutableStateFlow(localStorageHelper.isCartIvaEnabled())
    val cartIvaEnabled: StateFlow<Boolean> = _cartIvaEnabled.asStateFlow()

    private val _cartIgtfEnabled = MutableStateFlow(localStorageHelper.isCartIgtfEnabled())
    val cartIgtfEnabled: StateFlow<Boolean> = _cartIgtfEnabled.asStateFlow()

    // Market / Brecha Snapshots
    private val _rateSnapshots = MutableStateFlow(localStorageHelper.getRateSnapshots())
    val rateSnapshots: StateFlow<List<RateSnapshot>> = _rateSnapshots.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    // Checkout / Mixed Payment & Payment Profiles (Pago Móvil, Cuentas, Zelle, Binance Pay)
    private val _pagoMovilInfo = MutableStateFlow(localStorageHelper.getPagoMovilInfo())
    val pagoMovilInfo: StateFlow<PagoMovilInfo> = _pagoMovilInfo.asStateFlow()

    private val _paymentProfiles = MutableStateFlow(localStorageHelper.getPaymentProfiles())
    val paymentProfiles: StateFlow<List<PaymentMethodProfile>> = _paymentProfiles.asStateFlow()

    private val _selectedPaymentProfileId = MutableStateFlow<Long?>(
        localStorageHelper.getSelectedPaymentProfileId()
            ?: _paymentProfiles.value.firstOrNull()?.id
    )
    val selectedPaymentProfileId: StateFlow<Long?> = _selectedPaymentProfileId.asStateFlow()

    // Custom Brand Logo & Name for Ultra / Business Share Cards
    private val _customBrandName = MutableStateFlow(localStorageHelper.getCustomBrandName())
    val customBrandName: StateFlow<String> = _customBrandName.asStateFlow()

    private val _customBrandLogoRevision = MutableStateFlow(0L)
    val customBrandLogoRevision: StateFlow<Long> = _customBrandLogoRevision.asStateFlow()

    // Privacy-safe Analytics
    private val _analyticsEnabled = MutableStateFlow(localStorageHelper.isAnalyticsEnabled())
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()

    // Persistent Live-Rate Notification (Ultra opt-in)
    private val _persistentNotifEnabled = MutableStateFlow(localStorageHelper.isPersistentRateNotifEnabled())
    val persistentNotifEnabled: StateFlow<Boolean> = _persistentNotifEnabled.asStateFlow()

    fun setAnalyticsEnabled(enabled: Boolean) {
        _analyticsEnabled.value = enabled
        AnalyticsTracker.setCollectionEnabled(getApplication(), enabled)
    }

    fun setPersistentRateNotifEnabled(enabled: Boolean): Boolean {
        if (enabled && !isUltraUser.value) return false
        _persistentNotifEnabled.value = enabled
        localStorageHelper.setPersistentRateNotifEnabled(enabled)
        PersistentRateNotificationManager.sync(getApplication())
        RateSyncWorker.scheduleOrCancel(getApplication(), enabled)
        AnalyticsTracker.logAction(
            getApplication(),
            if (enabled) "persistent_notif_enabled" else "persistent_notif_disabled"
        )
        return true
    }

    // Selection & Dynamic Calculator state
    var selectedRateKey by mutableStateOf("USD_BCV")
        private set

    var isVesAsInput by mutableStateOf(false)
        private set

    var foreignAmount by mutableStateOf("")
        private set

    var vesAmount by mutableStateOf("")
        private set

    var previewTomorrowBcv by mutableStateOf(false)
        private set

    init {
        _themeMode.value = repository.getLocalTheme()

        val cached = repository.getCachedRatesBundle()
        val cachedCopRate = RateInfo(
            key = "COP",
            label = "Peso Colombiano",
            symbol = "COP",
            rate = if (cached.copPerUsd > 0.0) cached.usdBcv.rate / cached.copPerUsd else 0.0,
            dateUpdated = cached.copDate,
            isLocalOffline = true
        )
        _uiState.value = TasaVeUiState(
            isLoading = false,
            usdBcv = cached.usdBcv,
            eurBcv = cached.eurBcv,
            usdtParalelo = cached.usdtParalelo,
            nextUsdBcv = cached.nextUsdBcv,
            nextEurBcv = cached.nextEurBcv,
            nextBcvDateIso = cached.nextBcvDateIso,
            copPerUsd = cached.copPerUsd,
            copRate = cachedCopRate,
            isFromCache = true
        )
        recalculateVES()
        PersistentRateNotificationManager.sync(application)
        RateSyncWorker.scheduleOrCancel(application, _persistentNotifEnabled.value)
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
                    if (result.nextUsdBcv == null) {
                        previewTomorrowBcv = false
                    }
                    _uiState.value = TasaVeUiState(
                        isLoading = false,
                        usdBcv = result.usdBcv,
                        eurBcv = result.eurBcv,
                        usdtParalelo = result.usdtParalelo,
                        nextUsdBcv = result.nextUsdBcv,
                        nextEurBcv = result.nextEurBcv,
                        nextBcvDateIso = result.nextBcvDateIso,
                        copPerUsd = result.copPerUsd,
                        copRate = result.copRate,
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
        AnalyticsTracker.logConverterRateSelected(getApplication(), key)
    }

    fun setTomorrowBcvPreview(useTomorrow: Boolean) {
        if (useTomorrow && _uiState.value.nextUsdBcv == null) return
        previewTomorrowBcv = useTomorrow
        recalculateVES()
    }

    fun getEffectiveUsdBcv(): RateInfo? {
        val state = _uiState.value
        return if (previewTomorrowBcv && state.nextUsdBcv != null) state.nextUsdBcv else state.usdBcv
    }

    fun getEffectiveEurBcv(): RateInfo? {
        val state = _uiState.value
        return if (previewTomorrowBcv && state.nextEurBcv != null) state.nextEurBcv else state.eurBcv
    }

    fun getEffectiveCopRate(): RateInfo? {
        val state = _uiState.value
        val copPerUsd = state.copPerUsd
        val usd = getEffectiveUsdBcv() ?: return state.copRate
        return if (copPerUsd > 0.0) {
            (state.copRate ?: RateInfo(
                key = "COP",
                label = "Peso Colombiano",
                symbol = "COP",
                rate = usd.rate / copPerUsd,
                dateUpdated = usd.dateUpdated,
                isLocalOffline = state.isFromCache
            )).copy(rate = usd.rate / copPerUsd)
        } else state.copRate
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

    fun refreshRemainingFreeVoiceUses() {
        _remainingFreeVoiceUses.value = localStorageHelper.getRemainingFreeVoiceUses()
    }

    fun setVoiceTtsEnabled(enabled: Boolean) {
        _voiceTtsEnabled.value = enabled
        localStorageHelper.setVoiceTtsEnabled(enabled)
    }

    fun applyVoiceConversion(parsed: ParsedVoiceConversion) {
        selectedRateKey = parsed.rateKey
        previewTomorrowBcv = parsed.useTomorrowBcv &&
            _uiState.value.nextUsdBcv != null &&
            parsed.rateKey != "USDT"
        isVesAsInput = parsed.inputIsVes
        val formattedInput = if (parsed.amount % 1.0 == 0.0 && parsed.amount < 1_000_000_000.0) {
            parsed.amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", parsed.amount)
        }
        if (parsed.inputIsVes) {
            updateVesAmount(formattedInput)
        } else {
            updateForeignAmount(formattedInput)
        }
        if (!isUltraUser.value) {
            _remainingFreeVoiceUses.value = localStorageHelper.recordFreeVoiceUsed()
        }
        AnalyticsTracker.logVoiceUsed(
            context = getApplication(),
            isUltra = isUltraUser.value,
            rateKey = parsed.rateKey,
            source = "main_topbar"
        )
    }

    fun grantRewardedScans(extra: Int = UltraMath.REWARDED_SCAN_BONUS): Int {
        val nextRemaining = localStorageHelper.grantRewardedScanBonus(extra)
        _remainingFreeScans.value = nextRemaining
        return nextRemaining
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
        if (!isUltraUser.value) {
            _remainingFreeScans.value = localStorageHelper.recordFreeScanUsed()
        }
    }

    // --- Shopping Cart Actions (Free up to 5 items; Ultra unlimited + IVA/IGTF) ---
    fun addCartItem(
        name: String,
        amount: Double,
        isVes: Boolean,
        rateKey: String = "USD_BCV",
        quantity: Int = 1,
        fromScanner: Boolean = false
    ): Boolean {
        if (!UltraMath.canAddCartItem(_cartItems.value.size, isUltraUser.value)) {
            return false
        }

        val usdBcvRate = getEffectiveUsdBcv()?.rate ?: 1.0
        val rate = when (rateKey) {
            "EUR_BCV" -> getEffectiveEurBcv()?.rate
            "USDT" -> _uiState.value.usdtParalelo?.rate
            else -> usdBcvRate
        } ?: getSelectedRateValue().takeIf { it > 0 } ?: 1.0

        val unitUsd = if (isVes) {
            if (usdBcvRate > 0) amount / usdBcvRate else 0.0
        } else {
            if (rateKey == "USD_BCV" || usdBcvRate <= 0.0) amount else (amount * rate) / usdBcvRate
        }
        if (unitUsd <= 0.0) return false

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
        if (fromScanner && !isUltraUser.value) {
            _remainingFreeScans.value = localStorageHelper.recordFreeScanUsed()
        }
        return true
    }

    fun editCartItem(id: Long, name: String, amount: Double, isVes: Boolean, quantity: Int) {
        val usdBcvRate = getEffectiveUsdBcv()?.rate ?: getSelectedRateValue().takeIf { it > 0 } ?: 1.0
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

    fun toggleCartIva(): Boolean {
        if (!isUltraUser.value) return false
        val next = !_cartIvaEnabled.value
        _cartIvaEnabled.value = next
        localStorageHelper.setCartIvaEnabled(next)
        return true
    }

    fun toggleCartIgtf(): Boolean {
        if (!isUltraUser.value) return false
        val next = !_cartIgtfEnabled.value
        _cartIgtfEnabled.value = next
        localStorageHelper.setCartIgtfEnabled(next)
        return true
    }

    // --- Payment Profiles (Free: 1 profile; Ultra: unlimited) ---
    fun savePaymentProfile(profile: PaymentMethodProfile): Boolean {
        val current = _paymentProfiles.value.toMutableList()
        val isExisting = profile.id > 0L && current.any { it.id == profile.id }
        if (!isExisting && !UltraMath.canAddPaymentProfile(current.size, isUltraUser.value)) {
            return false
        }

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
        return true
    }

    fun deletePaymentProfile(id: Long) {
        val next = _paymentProfiles.value.filterNot { it.id == id }
        _paymentProfiles.value = next
        localStorageHelper.savePaymentProfiles(next)
        if (_selectedPaymentProfileId.value == id) {
            selectPaymentProfile(next.firstOrNull()?.id)
        } else {
            TasaVeRatesWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun selectPaymentProfile(id: Long?) {
        _selectedPaymentProfileId.value = id
        localStorageHelper.setSelectedPaymentProfileId(id)
        TasaVeRatesWidgetProvider.updateAllWidgets(getApplication())
    }

    fun savePagoMovilInfo(info: PagoMovilInfo) {
        _pagoMovilInfo.value = info
        localStorageHelper.savePagoMovilInfo(info)
    }

    fun saveCustomBrandName(name: String) {
        val cleaned = name.trim().take(40)
        _customBrandName.value = cleaned
        localStorageHelper.saveCustomBrandName(cleaned)
    }

    fun importCustomBrandLogo(uri: Uri): Boolean {
        val ok = ShareCardGenerator.importCustomBrandLogo(getApplication(), uri)
        if (ok) {
            _customBrandLogoRevision.value += 1L
        }
        return ok
    }

    fun removeCustomBrandLogo() {
        ShareCardGenerator.deleteCustomBrandLogo(getApplication())
        _customBrandLogoRevision.value += 1L
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
            "COP" -> "COP"
            else -> when (item.currencyTo) {
                "$" -> "USD_BCV"
                "€" -> "EUR_BCV"
                "₮" -> "USDT"
                "COP" -> "COP"
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
            "USD_BCV" -> getEffectiveUsdBcv()
            "EUR_BCV" -> getEffectiveEurBcv()
            "USDT" -> state.usdtParalelo
            "COP" -> getEffectiveCopRate()
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
