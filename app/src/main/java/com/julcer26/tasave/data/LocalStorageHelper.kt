package com.julcer26.tasave.data

import android.content.Context
import android.content.SharedPreferences
import com.julcer26.tasave.ultra.AlertSettings
import com.julcer26.tasave.ultra.CartItem
import com.julcer26.tasave.ultra.PagoMovilInfo
import com.julcer26.tasave.ultra.PaymentMethodProfile
import com.julcer26.tasave.ultra.PaymentMethodType
import com.julcer26.tasave.ultra.RateSnapshot
import com.julcer26.tasave.ultra.UltraMath
import com.julcer26.tasave.ultra.VENEZUELAN_BANKS
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CachedRatesBundle(
    val usdBcv: RateInfo,
    val eurBcv: RateInfo,
    val usdtParalelo: RateInfo,
    val nextUsdBcv: RateInfo? = null,
    val nextEurBcv: RateInfo? = null,
    val nextBcvDateIso: String? = null
)

class LocalStorageHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tasa_ve_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USD_BCV_RATE = "usd_bcv_rate"
        private const val KEY_USD_BCV_RATE_EXACT = "usd_bcv_rate_exact"
        private const val KEY_USD_BCV_DATE = "usd_bcv_date"
        private const val KEY_USD_BCV_EFFECTIVE = "usd_bcv_effective"
        private const val KEY_USD_BCV_CHANGE_PCT = "usd_bcv_change_pct"

        private const val KEY_EUR_BCV_RATE = "eur_bcv_rate"
        private const val KEY_EUR_BCV_RATE_EXACT = "eur_bcv_rate_exact"
        private const val KEY_EUR_BCV_DATE = "eur_bcv_date"
        private const val KEY_EUR_BCV_EFFECTIVE = "eur_bcv_effective"
        private const val KEY_EUR_BCV_CHANGE_PCT = "eur_bcv_change_pct"

        private const val KEY_USDT_RATE = "usdt_rate"
        private const val KEY_USDT_RATE_EXACT = "usdt_rate_exact"
        private const val KEY_USDT_DATE = "usdt_date"

        private const val KEY_NEXT_USD_BCV_EXACT = "next_usd_bcv_exact"
        private const val KEY_NEXT_EUR_BCV_EXACT = "next_eur_bcv_exact"
        private const val KEY_NEXT_BCV_DATE_ISO = "next_bcv_date_iso"

        private const val KEY_LAST_CACHED_TIME = "last_cached_time"
        private const val KEY_THEME = "theme"
        private const val KEY_IS_ULTRA_USER = "is_ultra_user"
        private const val KEY_SCAN_DATE = "scanner_last_date"
        private const val KEY_SCAN_COUNT_TODAY = "scanner_count_today"
        private const val KEY_SCAN_BONUS_TODAY = "scanner_bonus_today"

        // Ultra local persistence keys
        private const val KEY_CART_ITEMS_JSON = "ultra_cart_items_json"
        private const val KEY_CART_IVA = "ultra_cart_iva"
        private const val KEY_CART_IGTF = "ultra_cart_igtf"
        private const val KEY_RATE_SNAPSHOTS_JSON = "ultra_rate_snapshots_json"
        private const val KEY_ALERT_ENABLED = "ultra_alert_enabled"
        private const val KEY_ALERT_USD = "ultra_alert_usd"
        private const val KEY_ALERT_EUR = "ultra_alert_eur"
        private const val KEY_ALERT_USDT = "ultra_alert_usdt"
        private const val KEY_ALERT_THRESHOLD = "ultra_alert_threshold"
        private const val KEY_ALERT_LAST_USD = "ultra_alert_last_usd"
        private const val KEY_ALERT_LAST_EUR = "ultra_alert_last_eur"
        private const val KEY_ALERT_LAST_USDT = "ultra_alert_last_usdt"
        private const val KEY_PM_BANK = "ultra_pm_bank"
        private const val KEY_PM_DOC = "ultra_pm_doc"
        private const val KEY_PM_PHONE = "ultra_pm_phone"
        private const val KEY_PAYMENT_PROFILES_JSON = "ultra_payment_profiles_json"
        private const val KEY_SELECTED_PAYMENT_PROFILE_ID = "ultra_selected_payment_profile_id"
        private const val KEY_REMOTE_HISTORY_JSON = "ultra_remote_history_json"
        private const val KEY_REMOTE_HISTORY_TS = "ultra_remote_history_ts"
        private const val KEY_DEBUG_TIER = "debug_tier_override"
        private const val KEY_APP_LAUNCH_COUNT = "app_launch_count"
        private const val KEY_AD_FREE_PASS_UNTIL_MS = "rewarded_ad_free_until_ms"
        private const val KEY_CUSTOM_BRAND_NAME = "ultra_custom_brand_name"
        private const val KEY_WIDGET_CALC_RATE = "widget_calc_rate_key"
        private const val KEY_WIDGET_CALC_IS_VES = "widget_calc_is_ves"
        private const val KEY_WIDGET_CALC_INPUT = "widget_calc_input"

        const val MAX_FREE_SCANS_PER_DAY = 3

        private const val CACHE_EXPIRATION_MS = 4 * 60 * 60 * 1000L // 4 Hours
        private const val HISTORY_CACHE_EXPIRATION_MS = 6 * 60 * 60 * 1000L // 6 Hours

        // Fallbacks logic in case of absolute empty first run and no network
        const val FALLBACK_USD = 850.00
        const val FALLBACK_EUR = 950.00
        const val FALLBACK_USDT = 1000.00
    }

    fun isCacheExpired(): Boolean {
        val lastCached = prefs.getLong(KEY_LAST_CACHED_TIME, 0L)
        val elapsed = System.currentTimeMillis() - lastCached
        return elapsed > CACHE_EXPIRATION_MS
    }

    fun hasCachedData(): Boolean {
        return prefs.contains(KEY_USD_BCV_RATE) || prefs.contains(KEY_USD_BCV_RATE_EXACT)
    }

    fun saveRates(
        usd: Double,
        usdDate: String,
        eur: Double,
        eurDate: String,
        usdt: Double,
        usdtDate: String,
        usdEffectiveIso: String? = null,
        eurEffectiveIso: String? = null,
        usdChangePct: Double? = null,
        eurChangePct: Double? = null,
        nextUsd: Double? = null,
        nextEur: Double? = null,
        nextBcvDateIso: String? = null
    ) {
        prefs.edit().apply {
            putFloat(KEY_USD_BCV_RATE, usd.toFloat())
            putString(KEY_USD_BCV_RATE_EXACT, usd.toString())
            putString(KEY_USD_BCV_DATE, usdDate)
            if (usdEffectiveIso != null) putString(KEY_USD_BCV_EFFECTIVE, usdEffectiveIso) else remove(KEY_USD_BCV_EFFECTIVE)
            if (usdChangePct != null) putString(KEY_USD_BCV_CHANGE_PCT, usdChangePct.toString()) else remove(KEY_USD_BCV_CHANGE_PCT)

            putFloat(KEY_EUR_BCV_RATE, eur.toFloat())
            putString(KEY_EUR_BCV_RATE_EXACT, eur.toString())
            putString(KEY_EUR_BCV_DATE, eurDate)
            if (eurEffectiveIso != null) putString(KEY_EUR_BCV_EFFECTIVE, eurEffectiveIso) else remove(KEY_EUR_BCV_EFFECTIVE)
            if (eurChangePct != null) putString(KEY_EUR_BCV_CHANGE_PCT, eurChangePct.toString()) else remove(KEY_EUR_BCV_CHANGE_PCT)

            putFloat(KEY_USDT_RATE, usdt.toFloat())
            putString(KEY_USDT_RATE_EXACT, usdt.toString())
            putString(KEY_USDT_DATE, usdtDate)

            if (nextUsd != null && nextUsd > 0.0 && nextEur != null && nextEur > 0.0 && !nextBcvDateIso.isNullOrBlank()) {
                putString(KEY_NEXT_USD_BCV_EXACT, nextUsd.toString())
                putString(KEY_NEXT_EUR_BCV_EXACT, nextEur.toString())
                putString(KEY_NEXT_BCV_DATE_ISO, nextBcvDateIso)
            } else {
                remove(KEY_NEXT_USD_BCV_EXACT)
                remove(KEY_NEXT_EUR_BCV_EXACT)
                remove(KEY_NEXT_BCV_DATE_ISO)
            }

            putLong(KEY_LAST_CACHED_TIME, System.currentTimeMillis())
            apply()
        }
        appendRateSnapshot(usd, eur, usdt)
    }

    private fun readExactDouble(exactKey: String, legacyFloatKey: String?, fallback: Double): Double {
        val exact = prefs.getString(exactKey, null)?.toDoubleOrNull()
        if (exact != null && exact > 0.0) return exact
        if (legacyFloatKey != null && prefs.contains(legacyFloatKey)) {
            return prefs.getFloat(legacyFloatKey, fallback.toFloat()).toDouble()
        }
        return fallback
    }

    fun getRates(): Triple<RateInfo, RateInfo, RateInfo> {
        val bundle = getAllCachedRates()
        return Triple(bundle.usdBcv, bundle.eurBcv, bundle.usdtParalelo)
    }

    fun getAllCachedRates(): CachedRatesBundle {
        val usd = readExactDouble(KEY_USD_BCV_RATE_EXACT, KEY_USD_BCV_RATE, FALLBACK_USD)
        val usdDate = prefs.getString(KEY_USD_BCV_DATE, "N/D") ?: "N/D"
        val usdEff = prefs.getString(KEY_USD_BCV_EFFECTIVE, null)
        val usdPct = prefs.getString(KEY_USD_BCV_CHANGE_PCT, null)?.toDoubleOrNull()

        val eur = readExactDouble(KEY_EUR_BCV_RATE_EXACT, KEY_EUR_BCV_RATE, FALLBACK_EUR)
        val eurDate = prefs.getString(KEY_EUR_BCV_DATE, "N/D") ?: "N/D"
        val eurEff = prefs.getString(KEY_EUR_BCV_EFFECTIVE, null)
        val eurPct = prefs.getString(KEY_EUR_BCV_CHANGE_PCT, null)?.toDoubleOrNull()

        val usdt = readExactDouble(KEY_USDT_RATE_EXACT, KEY_USDT_RATE, FALLBACK_USDT)
        val usdtDate = prefs.getString(KEY_USDT_DATE, "N/D") ?: "N/D"

        val isLocalOffline = !hasCachedData()

        val usdInfo = RateInfo("USD_BCV", "Dólar BCV", "$", usd, usdDate, isLocalOffline, usdEff, usdPct)
        val eurInfo = RateInfo("EUR_BCV", "Euro BCV", "€", eur, eurDate, isLocalOffline, eurEff, eurPct)
        val usdtInfo = RateInfo("USDT", "USDT Binance", "₮", usdt, usdtDate, isLocalOffline)

        val nextDateIso = prefs.getString(KEY_NEXT_BCV_DATE_ISO, null)
        val nextUsdVal = prefs.getString(KEY_NEXT_USD_BCV_EXACT, null)?.toDoubleOrNull()
        val nextEurVal = prefs.getString(KEY_NEXT_EUR_BCV_EXACT, null)?.toDoubleOrNull()
        val todayCaracas = todayCaracasIsoDate()

        val validNext = nextDateIso != null &&
            nextDateIso > todayCaracas &&
            nextUsdVal != null && nextUsdVal > 0.0 &&
            nextEurVal != null && nextEurVal > 0.0

        val nextUsdInfo = if (validNext) {
            RateInfo("USD_BCV", "Dólar BCV", "$", nextUsdVal!!, usdDate, isLocalOffline, nextDateIso, usdPct)
        } else null
        val nextEurInfo = if (validNext) {
            RateInfo("EUR_BCV", "Euro BCV", "€", nextEurVal!!, eurDate, isLocalOffline, nextDateIso, eurPct)
        } else null

        return CachedRatesBundle(
            usdBcv = usdInfo,
            eurBcv = eurInfo,
            usdtParalelo = usdtInfo,
            nextUsdBcv = nextUsdInfo,
            nextEurBcv = nextEurInfo,
            nextBcvDateIso = if (validNext) nextDateIso else null
        )
    }

    private fun todayCaracasIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("America/Caracas")
        return sdf.format(Date())
    }

    fun getTheme(): String {
        return prefs.getString(KEY_THEME, "DARK") ?: "DARK"
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun isUltraUser(): Boolean = prefs.getBoolean(KEY_IS_ULTRA_USER, false)

    fun setUltraUser(isUltra: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ULTRA_USER, isUltra).apply()
    }

    fun getDebugTierOverride(): String = prefs.getString(KEY_DEBUG_TIER, "ULTRA") ?: "ULTRA"

    fun setDebugTierOverride(tier: String) {
        prefs.edit().putString(KEY_DEBUG_TIER, tier).apply()
    }

    private fun todayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getRemainingFreeScans(): Int {
        val today = todayDateKey()
        val savedDate = prefs.getString(KEY_SCAN_DATE, null)
        if (savedDate != today) {
            return MAX_FREE_SCANS_PER_DAY
        }
        val used = prefs.getInt(KEY_SCAN_COUNT_TODAY, 0)
        val bonus = prefs.getInt(KEY_SCAN_BONUS_TODAY, 0)
        return ((MAX_FREE_SCANS_PER_DAY + bonus) - used).coerceAtLeast(0)
    }

    fun recordFreeScanUsed(): Int {
        val today = todayDateKey()
        val savedDate = prefs.getString(KEY_SCAN_DATE, null)
        val currentUsed = if (savedDate == today) prefs.getInt(KEY_SCAN_COUNT_TODAY, 0) else 0
        val currentBonus = if (savedDate == today) prefs.getInt(KEY_SCAN_BONUS_TODAY, 0) else 0
        val totalAllowed = MAX_FREE_SCANS_PER_DAY + currentBonus
        val nextUsed = (currentUsed + 1).coerceAtMost(totalAllowed)
        prefs.edit()
            .putString(KEY_SCAN_DATE, today)
            .putInt(KEY_SCAN_COUNT_TODAY, nextUsed)
            .putInt(KEY_SCAN_BONUS_TODAY, currentBonus)
            .apply()
        return (totalAllowed - nextUsed).coerceAtLeast(0)
    }

    fun grantRewardedScanBonus(extraScans: Int = UltraMath.REWARDED_SCAN_BONUS): Int {
        val today = todayDateKey()
        val savedDate = prefs.getString(KEY_SCAN_DATE, null)
        val currentUsed = if (savedDate == today) prefs.getInt(KEY_SCAN_COUNT_TODAY, 0) else 0
        val currentBonus = if (savedDate == today) prefs.getInt(KEY_SCAN_BONUS_TODAY, 0) else 0
        val nextBonus = currentBonus + extraScans.coerceAtLeast(1)
        prefs.edit()
            .putString(KEY_SCAN_DATE, today)
            .putInt(KEY_SCAN_COUNT_TODAY, currentUsed)
            .putInt(KEY_SCAN_BONUS_TODAY, nextBonus)
            .apply()
        return ((MAX_FREE_SCANS_PER_DAY + nextBonus) - currentUsed).coerceAtLeast(0)
    }

    // --- Cart Persistence ---
    fun getCartItems(): List<CartItem> {
        val raw = prefs.getString(KEY_CART_ITEMS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        CartItem(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            unitPriceUsd = obj.getDouble("unitPriceUsd"),
                            quantity = obj.optInt("quantity", 1)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCartItems(items: List<CartItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("unitPriceUsd", item.unitPriceUsd)
                    put("quantity", item.quantity)
                }
            )
        }
        prefs.edit().putString(KEY_CART_ITEMS_JSON, arr.toString()).apply()
    }

    fun isCartIvaEnabled(): Boolean = prefs.getBoolean(KEY_CART_IVA, false)
    fun setCartIvaEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CART_IVA, enabled).apply()
    }

    fun isCartIgtfEnabled(): Boolean = prefs.getBoolean(KEY_CART_IGTF, false)
    fun setCartIgtfEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CART_IGTF, enabled).apply()
    }

    // --- Rate Snapshots for Local Chart ---
    fun getRateSnapshots(): List<RateSnapshot> {
        return parseSnapshotsJson(prefs.getString(KEY_RATE_SNAPSHOTS_JSON, null))
    }

    private fun parseSnapshotsJson(raw: String?): List<RateSnapshot> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        RateSnapshot(
                            timestampMs = obj.getLong("ts"),
                            label = obj.getString("label"),
                            usdBcv = obj.getDouble("usd"),
                            eurBcv = obj.getDouble("eur"),
                            usdt = obj.getDouble("usdt")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeSnapshotsJson(items: List<RateSnapshot>): String {
        val arr = JSONArray()
        items.forEach { snap ->
            arr.put(
                JSONObject().apply {
                    put("ts", snap.timestampMs)
                    put("label", snap.label)
                    put("usd", snap.usdBcv)
                    put("eur", snap.eurBcv)
                    put("usdt", snap.usdt)
                }
            )
        }
        return arr.toString()
    }

    private fun appendRateSnapshot(usd: Double, eur: Double, usdt: Double) {
        val existing = getRateSnapshots().toMutableList()
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(now))
        val last = existing.lastOrNull()
        val changed = last == null ||
            kotlin.math.abs(last.usdBcv - usd) > 0.01 ||
            kotlin.math.abs(last.eurBcv - eur) > 0.01 ||
            kotlin.math.abs(last.usdt - usdt) > 0.01 ||
            (now - last.timestampMs) > 60 * 60 * 1000L

        if (!changed) return

        existing.add(RateSnapshot(now, label, usd, eur, usdt))
        val trimmed = if (existing.size > 72) existing.takeLast(72) else existing
        prefs.edit().putString(KEY_RATE_SNAPSHOTS_JSON, encodeSnapshotsJson(trimmed)).apply()
    }

    fun isRemoteHistoryExpired(): Boolean {
        val last = prefs.getLong(KEY_REMOTE_HISTORY_TS, 0L)
        return (System.currentTimeMillis() - last) > HISTORY_CACHE_EXPIRATION_MS
    }

    fun getRemoteHistorySnapshots(): List<RateSnapshot> {
        return parseSnapshotsJson(prefs.getString(KEY_REMOTE_HISTORY_JSON, null))
    }

    fun saveRemoteHistorySnapshots(items: List<RateSnapshot>) {
        prefs.edit()
            .putString(KEY_REMOTE_HISTORY_JSON, encodeSnapshotsJson(items))
            .putLong(KEY_REMOTE_HISTORY_TS, System.currentTimeMillis())
            .apply()
    }

    // --- Rate Alert Preferences ---
    fun getAlertSettings(): AlertSettings = AlertSettings(
        enabled = prefs.getBoolean(KEY_ALERT_ENABLED, false),
        watchUsdBcv = prefs.getBoolean(KEY_ALERT_USD, true),
        watchEurBcv = prefs.getBoolean(KEY_ALERT_EUR, true),
        watchUsdt = prefs.getBoolean(KEY_ALERT_USDT, true),
        thresholdPercent = prefs.getFloat(KEY_ALERT_THRESHOLD, 1.0f).toDouble()
    )

    fun saveAlertSettings(settings: AlertSettings) {
        prefs.edit()
            .putBoolean(KEY_ALERT_ENABLED, settings.enabled)
            .putBoolean(KEY_ALERT_USD, settings.watchUsdBcv)
            .putBoolean(KEY_ALERT_EUR, settings.watchEurBcv)
            .putBoolean(KEY_ALERT_USDT, settings.watchUsdt)
            .putFloat(KEY_ALERT_THRESHOLD, settings.thresholdPercent.toFloat())
            .apply()
    }

    fun getLastNotifiedRates(): Triple<Double, Double, Double> {
        return Triple(
            prefs.getFloat(KEY_ALERT_LAST_USD, 0f).toDouble(),
            prefs.getFloat(KEY_ALERT_LAST_EUR, 0f).toDouble(),
            prefs.getFloat(KEY_ALERT_LAST_USDT, 0f).toDouble()
        )
    }

    fun saveLastNotifiedRates(usd: Double, eur: Double, usdt: Double) {
        prefs.edit()
            .putFloat(KEY_ALERT_LAST_USD, usd.toFloat())
            .putFloat(KEY_ALERT_LAST_EUR, eur.toFloat())
            .putFloat(KEY_ALERT_LAST_USDT, usdt.toFloat())
            .apply()
    }

    // --- Pago Móvil, Bank Account, Zelle & Binance Pay Multi-Profile Storage ---
    fun getPaymentProfiles(): List<PaymentMethodProfile> {
        val raw = prefs.getString(KEY_PAYMENT_PROFILES_JSON, null)
        if (!raw.isNullOrBlank()) {
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val typeStr = obj.optString("type", PaymentMethodType.PAGO_MOVIL.name)
                        val type = runCatching { PaymentMethodType.valueOf(typeStr) }
                            .getOrDefault(PaymentMethodType.PAGO_MOVIL)
                        add(
                            PaymentMethodProfile(
                                id = obj.getLong("id"),
                                alias = obj.optString("alias", ""),
                                type = type,
                                bankCode = obj.optString("bankCode", "0134"),
                                bankName = obj.optString("bankName", "Banesco"),
                                docPrefix = obj.optString("docPrefix", "V"),
                                docNumber = obj.optString("docNumber", ""),
                                phonePrefix = obj.optString("phonePrefix", "0414"),
                                phoneNumber = obj.optString("phoneNumber", ""),
                                accountNumber = obj.optString("accountNumber", ""),
                                accountHolder = obj.optString("accountHolder", ""),
                                digitalIdentifier = obj.optString("digitalIdentifier", "")
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        // Legacy migration from single PagoMovilInfo if any
        val legacy = getPagoMovilInfo()
        if (legacy.bank.isNotBlank() || legacy.documentId.isNotBlank() || legacy.phone.isNotBlank()) {
            val matchedBank = VENEZUELAN_BANKS.firstOrNull {
                it.name.contains(legacy.bank, ignoreCase = true) || legacy.bank.contains(it.code)
            }
            val cleanDoc = legacy.documentId.trim()
            val docPrefix = if (cleanDoc.isNotEmpty() && cleanDoc[0].uppercaseChar() in listOf('V', 'E', 'J', 'G', 'P', 'C')) {
                cleanDoc[0].uppercaseChar().toString()
            } else "V"
            val docNum = cleanDoc.filter { it.isDigit() }
            val phoneDigits = legacy.phone.filter { it.isDigit() }
            val phonePrefix = if (phoneDigits.length >= 4) phoneDigits.take(4) else "0414"
            val phoneNum = if (phoneDigits.length > 4) phoneDigits.drop(4) else phoneDigits

            val migrated = listOf(
                PaymentMethodProfile(
                    id = 1L,
                    alias = "Principal",
                    type = PaymentMethodType.PAGO_MOVIL,
                    bankCode = matchedBank?.code ?: "0134",
                    bankName = matchedBank?.name ?: legacy.bank.ifBlank { "Banesco" },
                    docPrefix = docPrefix,
                    docNumber = docNum,
                    phonePrefix = phonePrefix,
                    phoneNumber = phoneNum
                )
            )
            savePaymentProfiles(migrated)
            setSelectedPaymentProfileId(1L)
            return migrated
        }
        return emptyList()
    }

    fun savePaymentProfiles(profiles: List<PaymentMethodProfile>) {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("alias", p.alias)
                    put("type", p.type.name)
                    put("bankCode", p.bankCode)
                    put("bankName", p.bankName)
                    put("docPrefix", p.docPrefix)
                    put("docNumber", p.docNumber)
                    put("phonePrefix", p.phonePrefix)
                    put("phoneNumber", p.phoneNumber)
                    put("accountNumber", p.accountNumber)
                    put("accountHolder", p.accountHolder)
                    put("digitalIdentifier", p.digitalIdentifier)
                }
            )
        }
        prefs.edit().putString(KEY_PAYMENT_PROFILES_JSON, arr.toString()).apply()
    }

    fun getSelectedPaymentProfileId(): Long? {
        val id = prefs.getLong(KEY_SELECTED_PAYMENT_PROFILE_ID, -1L)
        return if (id <= 0L) null else id
    }

    fun setSelectedPaymentProfileId(id: Long?) {
        prefs.edit().putLong(KEY_SELECTED_PAYMENT_PROFILE_ID, id ?: -1L).apply()
    }

    // Legacy single Pago Móvil support
    fun getPagoMovilInfo(): PagoMovilInfo = PagoMovilInfo(
        bank = prefs.getString(KEY_PM_BANK, "") ?: "",
        documentId = prefs.getString(KEY_PM_DOC, "") ?: "",
        phone = prefs.getString(KEY_PM_PHONE, "") ?: ""
    )

    fun savePagoMovilInfo(info: PagoMovilInfo) {
        prefs.edit()
            .putString(KEY_PM_BANK, info.bank.trim())
            .putString(KEY_PM_DOC, info.documentId.trim())
            .putString(KEY_PM_PHONE, info.phone.trim())
            .apply()
    }

    fun getAppLaunchCount(): Int = prefs.getInt(KEY_APP_LAUNCH_COUNT, 0)

    fun incrementAndGetAppLaunchCount(): Int {
        val next = getAppLaunchCount() + 1
        prefs.edit().putInt(KEY_APP_LAUNCH_COUNT, next).apply()
        return next
    }

    fun getAdFreePassUntilMs(): Long = prefs.getLong(KEY_AD_FREE_PASS_UNTIL_MS, 0L)

    fun grantAdFreePassHours(hours: Int = 12, nowMs: Long = System.currentTimeMillis()): Long {
        val until = maxOf(getAdFreePassUntilMs(), nowMs) + hours * 3_600_000L
        prefs.edit().putLong(KEY_AD_FREE_PASS_UNTIL_MS, until).apply()
        return until
    }

    fun isAdFreePassActive(nowMs: Long = System.currentTimeMillis()): Boolean {
        return getAdFreePassUntilMs() > nowMs
    }

    fun getCustomBrandName(): String {
        return prefs.getString(KEY_CUSTOM_BRAND_NAME, "") ?: ""
    }

    fun saveCustomBrandName(name: String) {
        val cleaned = name.trim().take(40)
        prefs.edit().putString(KEY_CUSTOM_BRAND_NAME, cleaned).apply()
    }

    fun getWidgetCalcRateKey(): String = prefs.getString(KEY_WIDGET_CALC_RATE, "USD_BCV") ?: "USD_BCV"

    fun isWidgetCalcVesInput(): Boolean = prefs.getBoolean(KEY_WIDGET_CALC_IS_VES, false)

    fun getWidgetCalcInput(): String = prefs.getString(KEY_WIDGET_CALC_INPUT, "1") ?: "1"

    fun saveWidgetCalcState(rateKey: String, isVesInput: Boolean, input: String) {
        prefs.edit()
            .putString(KEY_WIDGET_CALC_RATE, rateKey)
            .putBoolean(KEY_WIDGET_CALC_IS_VES, isVesInput)
            .putString(KEY_WIDGET_CALC_INPUT, input)
            .apply()
    }
}
