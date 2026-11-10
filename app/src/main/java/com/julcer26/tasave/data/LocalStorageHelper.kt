package com.julcer26.tasave.data

import android.content.Context
import android.content.SharedPreferences

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

    fun isProUser(): Boolean {
        return prefs.getBoolean(KEY_IS_PRO_USER, false)
    }

    fun setProUser(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO_USER, isPro).apply()
    }
}
