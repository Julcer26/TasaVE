package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TasaVeRepository(
    context: Context,
    private val historyDao: ConversionHistoryDao
) {
    private val cacheHelper = LocalStorageHelper(context)
    
    // Explicit OkHttpClient configurations to prevent long network hangs
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit Instance
    private val apiService: DolarApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://ve.dolarapi.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(DolarApiService::class.java)
    }

    val history: Flow<List<ConversionHistory>> = historyDao.getHistory()

    fun getLocalTheme(): String = cacheHelper.getTheme()
    fun saveLocalTheme(theme: String) = cacheHelper.saveTheme(theme)

    fun getCachedRates(): Triple<RateInfo, RateInfo, RateInfo> {
        return cacheHelper.getRates()
    }

    suspend fun insertHistory(historyItem: ConversionHistory) = withContext(Dispatchers.IO) {
        historyDao.insert(historyItem)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }

    suspend fun fetchRates(forceRefresh: Boolean = false): TasaVeRatesResult = withContext(Dispatchers.IO) {
        // Try accessing local cache first if cache is not expired, data exists and we are not forcing.
        if (!forceRefresh && !cacheHelper.isCacheExpired() && cacheHelper.hasCachedData()) {
            val (usd, eur, usdt) = cacheHelper.getRates()
            return@withContext TasaVeRatesResult.Success(usd, eur, usdt, isFromCache = true)
        }

        try {
            coroutineScope {
                val usdDeferred = async { apiService.getUsdOficial() }
                val eurDeferred = async { apiService.getEurOficial() }
                val usdtDeferred = async { apiService.getUsdtParalelo() }

                val usdResponse = usdDeferred.await()
                val eurResponse = eurDeferred.await()
                val usdtResponse = usdtDeferred.await()

                // Save elements into Cache
                cacheHelper.saveRates(
                    usd = usdResponse.promedio,
                    usdDate = formatDate(usdResponse.fechaActualizacion) ?: getCurrentDateString(),
                    eur = eurResponse.promedio,
                    eurDate = formatDate(eurResponse.fechaActualizacion) ?: getCurrentDateString(),
                    usdt = usdtResponse.promedio,
                    usdtDate = formatDate(usdtResponse.fechaActualizacion) ?: getCurrentDateString()
                )

                val usdRate = RateInfo("USD_BCV", "Dólar BCV", "$", usdResponse.promedio, formatDate(usdResponse.fechaActualizacion) ?: getCurrentDateString(), false)
                val eurRate = RateInfo("EUR_BCV", "Euro BCV", "€", eurResponse.promedio, formatDate(eurResponse.fechaActualizacion) ?: getCurrentDateString(), false)
                val usdtRate = RateInfo("USDT", "USDT Binance", "₮", usdtResponse.promedio, formatDate(usdtResponse.fechaActualizacion) ?: getCurrentDateString(), false)

                TasaVeRatesResult.Success(usdRate, eurRate, usdtRate, isFromCache = false)
            }
        } catch (e: Exception) {
            Log.e("TasaVeRepository", "Error fetching from DolarAPI, falling back to local storage", e)
            // Resilience fallback: retrieve cached values, mark as offline if no connection
            val (usd, eur, usdt) = cacheHelper.getRates()
            TasaVeRatesResult.Success(
                usd.copy(isLocalOffline = true),
                eur.copy(isLocalOffline = true),
                usdt.copy(isLocalOffline = true),
                isFromCache = true,
                isOfflineError = true
            )
        }
    }

    private fun formatDate(dateStr: String?): String? {
        if (dateStr == null) return null
        return try {
            // Format from "2026-06-11T03:22:37Z" to "dd/MM/yyyy hh:mm a"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) {
                outputFormat.format(date)
            } else null
        } catch (e: Exception) {
            try {
                // Secondary check for standard ISO representation "2026-06-11T03:22:37Z"
                val altInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
                val date = altInput.parse(dateStr)
                if (date != null) {
                    outputFormat.format(date)
                } else null
            } catch (e2: Exception) {
                dateStr // Fallback to raw string
            }
        }
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault()).format(Date())
    }
}

sealed class TasaVeRatesResult {
    data class Success(
        val usdBcv: RateInfo,
        val eurBcv: RateInfo,
        val usdtParalelo: RateInfo,
        val isFromCache: Boolean,
        val isOfflineError: Boolean = false
    ) : TasaVeRatesResult()
    data class Error(val message: String) : TasaVeRatesResult()
}
