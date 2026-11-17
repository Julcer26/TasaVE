package com.julcer26.tasave.data

import android.content.Context
import android.content.SharedPreferences
import com.julcer26.tasave.ultra.AlertSettings
import com.julcer26.tasave.ultra.CartItem
import com.julcer26.tasave.ultra.PagoMovilInfo
import com.julcer26.tasave.ultra.RateSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalStorageHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tasa_ve_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USD_BCV_RATE = "usd_bcv_rate"
        private const val KEY_USD_BCV_DATE = "usd_bcv_date"
        private const val KEY_EUR_BCV_RATE = "eur_bcv_rate"
        private const val KEY_EUR_BCV_DATE = "eur_bcv_date"
        private const val KEY_USDT_RATE = "usdt_rate"
        private const val KEY_USDT_DATE = "usdt_date"
        private const val KEY_LAST_CACHED_TIME = "last_cached_time"
        private const val KEY_THEME = "theme"
        private const val KEY_IS_PRO_USER = "is_pro_user"
        private const val KEY_IS_ULTRA_USER = "is_ultra_user"
        private const val KEY_SCAN_DATE = "scanner_last_date"
        private const val KEY_SCAN_COUNT_TODAY = "scanner_count_today"

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
        private const val KEY_DEBUG_TIER = "debug_tier_override"

        const val MAX_FREE_SCANS_PER_DAY = 3

        private const val CACHE_EXPIRATION_MS = 4 * 60 * 60 * 1000L // 4 Hours

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
        return prefs.contains(KEY_USD_BCV_RATE)
    }

    fun saveRates(usd: Double, usdDate: String, eur: Double, eurDate: String, usdt: Double, usdtDate: String) {
        prefs.edit().apply {
            putFloat(KEY_USD_BCV_RATE, usd.toFloat())
            putString(KEY_USD_BCV_DATE, usdDate)
            putFloat(KEY_EUR_BCV_RATE, eur.toFloat())
            putString(KEY_EUR_BCV_DATE, eurDate)
            putFloat(KEY_USDT_RATE, usdt.toFloat())
            putString(KEY_USDT_DATE, usdtDate)
            putLong(KEY_LAST_CACHED_TIME, System.currentTimeMillis())
            apply()
        }
        appendRateSnapshot(usd, eur, usdt)
    }

    fun getRates(): Triple<RateInfo, RateInfo, RateInfo> {
        val usd = prefs.getFloat(KEY_USD_BCV_RATE, FALLBACK_USD.toFloat()).toDouble()
        val usdDate = prefs.getString(KEY_USD_BCV_DATE, "N/D") ?: "N/D"

        val eur = prefs.getFloat(KEY_EUR_BCV_RATE, FALLBACK_EUR.toFloat()).toDouble()
        val eurDate = prefs.getString(KEY_EUR_BCV_DATE, "N/D") ?: "N/D"

        val usdt = prefs.getFloat(KEY_USDT_RATE, FALLBACK_USDT.toFloat()).toDouble()
        val usdtDate = prefs.getString(KEY_USDT_DATE, "N/D") ?: "N/D"

        val isLocalOffline = !hasCachedData()

        val usdInfo = RateInfo("USD_BCV", "Dólar BCV", "$", usd, usdDate, isLocalOffline)
        val eurInfo = RateInfo("EUR_BCV", "Euro BCV", "€", eur, eurDate, isLocalOffline)
        val usdtInfo = RateInfo("USDT", "USDT Binance", "₮", usdt, usdtDate, isLocalOffline)

        return Triple(usdInfo, eurInfo, usdtInfo)
    }

    fun getTheme(): String {
        return prefs.getString(KEY_THEME, "DARK") ?: "DARK" // Default to DARK mode
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun isProUser(): Boolean = prefs.getBoolean(KEY_IS_PRO_USER, false)

    fun setProUser(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO_USER, isPro).apply()
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
        return (MAX_FREE_SCANS_PER_DAY - used).coerceAtLeast(0)
    }

    fun recordFreeScanUsed(): Int {
        val today = todayDateKey()
        val savedDate = prefs.getString(KEY_SCAN_DATE, null)
        val currentUsed = if (savedDate == today) prefs.getInt(KEY_SCAN_COUNT_TODAY, 0) else 0
        val nextUsed = (currentUsed + 1).coerceAtMost(MAX_FREE_SCANS_PER_DAY)
        prefs.edit()
            .putString(KEY_SCAN_DATE, today)
            .putInt(KEY_SCAN_COUNT_TODAY, nextUsed)
            .apply()
        return (MAX_FREE_SCANS_PER_DAY - nextUsed).coerceAtLeast(0)
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
        val raw = prefs.getString(KEY_RATE_SNAPSHOTS_JSON, null) ?: return emptyList()
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

    private fun appendRateSnapshot(usd: Double, eur: Double, usdt: Double) {
        val existing = getRateSnapshots().toMutableList()
        val now = System.currentTimeMillis()
        val label = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(now))
        val last = existing.lastOrNull()
        val changed = last == null ||
            kotlin.math.abs(last.usdBcv - usd) > 0.01 ||
            kotlin.math.abs(last.eurBcv - eur) > 0.01 ||
            kotlin.math.abs(last.usdt - usdt) > 0.01 ||
            (now - last.timestampMs) > 6 * 60 * 60 * 1000L

        if (!changed) return

        existing.add(RateSnapshot(now, label, usd, eur, usdt))
        val trimmed = if (existing.size > 14) existing.takeLast(14) else existing
        val arr = JSONArray()
        trimmed.forEach { snap ->
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
        prefs.edit().putString(KEY_RATE_SNAPSHOTS_JSON, arr.toString()).apply()
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

    // --- Pago Móvil Local Merchant Details ---
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
}
