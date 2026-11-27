package com.julcer26.tasave.data

import android.content.Context
import android.util.Log
import com.julcer26.tasave.ultra.RateSnapshot
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class ResolvedBcvRates(
    val todayUsd: Double,
    val todayEur: Double,
    val todayEffectiveIso: String?,
    val nextUsd: Double?,
    val nextEur: Double?,
    val nextEffectiveIso: String?,
    val usdChangePct: Double?,
    val eurChangePct: Double?
)

class TasaVeRepository(
    context: Context,
    private val historyDao: ConversionHistoryDao
) {
    private val cacheHelper = LocalStorageHelper(context)

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val apiService: DolarApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://ve.dolarapi.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DolarApiService::class.java)
    }

    val history: Flow<List<ConversionHistory>> = historyDao.getHistory()

    fun getLocalTheme(): String = cacheHelper.getTheme()
    fun saveLocalTheme(theme: String) = cacheHelper.saveTheme(theme)

    fun getCachedRatesBundle(): CachedRatesBundle = cacheHelper.getAllCachedRates()

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
        if (!forceRefresh && !cacheHelper.isCacheExpired() && cacheHelper.hasCachedData()) {
            val cached = cacheHelper.getAllCachedRates()
            val copRateInfo = RateInfo(
                key = "COP",
                label = "Peso Colombiano",
                symbol = "COP",
                rate = if (cached.copPerUsd > 0.0) cached.usdBcv.rate / cached.copPerUsd else 0.0,
                dateUpdated = cached.copDate,
                isLocalOffline = false
            )
            return@withContext TasaVeRatesResult.Success(
                usdBcv = cached.usdBcv,
                eurBcv = cached.eurBcv,
                usdtParalelo = cached.usdtParalelo,
                nextUsdBcv = cached.nextUsdBcv,
                nextEurBcv = cached.nextEurBcv,
                nextBcvDateIso = cached.nextBcvDateIso,
                copPerUsd = cached.copPerUsd,
                copRate = copRateInfo,
                isFromCache = true
            )
        }

        try {
            coroutineScope {
                val usdDeferred = async { apiService.getUsdOficial() }
                val eurDeferred = async { apiService.getEurOficial() }
                val usdtDeferred = async { apiService.getUsdtParalelo() }
                val bcvDetailedDeferred = async {
                    runCatching { apiService.getBcvDetailed() }.getOrNull()
                }
                val copTrmDeferred = async {
                    runCatching { apiService.getColombiaTrm() }.getOrNull()
                }

                val usdResponse = usdDeferred.await()
                val eurResponse = eurDeferred.await()
                val usdtResponse = usdtDeferred.await()
                val bcvDetailed = bcvDetailedDeferred.await()
                val copTrm = copTrmDeferred.await()

                val cachedFallback = cacheHelper.getAllCachedRates()
                val resolvedCopPerUsd: Double
                val resolvedCopDate: String
                val copTrmValor = copTrm?.valor
                if (copTrmValor != null && copTrmValor > 0.0) {
                    resolvedCopPerUsd = copTrmValor
                    resolvedCopDate = formatDate(copTrm.fechaActualizacion) ?: getCurrentDateString()
                } else {
                    val openErCop = runCatching { apiService.getOpenErUsdRates() }.getOrNull()?.rates?.get("COP")
                    if (openErCop != null && openErCop > 0.0) {
                        resolvedCopPerUsd = openErCop
                        resolvedCopDate = getCurrentDateString()
                    } else {
                        resolvedCopPerUsd = cachedFallback.copPerUsd
                        resolvedCopDate = cachedFallback.copDate
                    }
                }

                val todayCaracas = todayCaracasIsoDate()
                val resolvedBcv = resolveBcvTodayAndNext(
                    bcvResp = bcvDetailed,
                    fallbackUsd = usdResponse.promedio,
                    fallbackEur = eurResponse.promedio,
                    todayIso = todayCaracas
                )

                val usdDate = formatDate(usdResponse.fechaActualizacion) ?: getCurrentDateString()
                val eurDate = formatDate(eurResponse.fechaActualizacion) ?: getCurrentDateString()
                val usdtDate = formatDate(usdtResponse.fechaActualizacion) ?: getCurrentDateString()

                cacheHelper.saveRates(
                    usd = resolvedBcv.todayUsd,
                    usdDate = usdDate,
                    eur = resolvedBcv.todayEur,
                    eurDate = eurDate,
                    usdt = usdtResponse.promedio,
                    usdtDate = usdtDate,
                    usdEffectiveIso = resolvedBcv.todayEffectiveIso,
                    eurEffectiveIso = resolvedBcv.todayEffectiveIso,
                    usdChangePct = resolvedBcv.usdChangePct,
                    eurChangePct = resolvedBcv.eurChangePct,
                    nextUsd = resolvedBcv.nextUsd,
                    nextEur = resolvedBcv.nextEur,
                    nextBcvDateIso = resolvedBcv.nextEffectiveIso,
                    copPerUsd = resolvedCopPerUsd,
                    copDate = resolvedCopDate
                )

                val usdRate = RateInfo(
                    key = "USD_BCV",
                    label = "Dólar BCV",
                    symbol = "$",
                    rate = resolvedBcv.todayUsd,
                    dateUpdated = usdDate,
                    isLocalOffline = false,
                    effectiveDateIso = resolvedBcv.todayEffectiveIso,
                    changePercent = resolvedBcv.usdChangePct
                )
                val eurRate = RateInfo(
                    key = "EUR_BCV",
                    label = "Euro BCV",
                    symbol = "€",
                    rate = resolvedBcv.todayEur,
                    dateUpdated = eurDate,
                    isLocalOffline = false,
                    effectiveDateIso = resolvedBcv.todayEffectiveIso,
                    changePercent = resolvedBcv.eurChangePct
                )
                val usdtRate = RateInfo(
                    key = "USDT",
                    label = "USDT Binance",
                    symbol = "₮",
                    rate = usdtResponse.promedio,
                    dateUpdated = usdtDate,
                    isLocalOffline = false
                )
                val copRate = RateInfo(
                    key = "COP",
                    label = "Peso Colombiano",
                    symbol = "COP",
                    rate = if (resolvedCopPerUsd > 0.0) resolvedBcv.todayUsd / resolvedCopPerUsd else 0.0,
                    dateUpdated = resolvedCopDate,
                    isLocalOffline = false
                )

                val nextUsdRate = if (resolvedBcv.nextUsd != null && resolvedBcv.nextEffectiveIso != null) {
                    RateInfo(
                        key = "USD_BCV",
                        label = "Dólar BCV",
                        symbol = "$",
                        rate = resolvedBcv.nextUsd,
                        dateUpdated = formatIsoShortDate(resolvedBcv.nextEffectiveIso),
                        isLocalOffline = false,
                        effectiveDateIso = resolvedBcv.nextEffectiveIso,
                        changePercent = resolvedBcv.usdChangePct
                    )
                } else null

                val nextEurRate = if (resolvedBcv.nextEur != null && resolvedBcv.nextEffectiveIso != null) {
                    RateInfo(
                        key = "EUR_BCV",
                        label = "Euro BCV",
                        symbol = "€",
                        rate = resolvedBcv.nextEur,
                        dateUpdated = formatIsoShortDate(resolvedBcv.nextEffectiveIso),
                        isLocalOffline = false,
                        effectiveDateIso = resolvedBcv.nextEffectiveIso,
                        changePercent = resolvedBcv.eurChangePct
                    )
                } else null

                TasaVeRatesResult.Success(
                    usdBcv = usdRate,
                    eurBcv = eurRate,
                    usdtParalelo = usdtRate,
                    nextUsdBcv = nextUsdRate,
                    nextEurBcv = nextEurRate,
                    nextBcvDateIso = resolvedBcv.nextEffectiveIso,
                    copPerUsd = resolvedCopPerUsd,
                    copRate = copRate,
                    isFromCache = false
                )
            }
        } catch (e: Exception) {
            Log.e("TasaVeRepository", "Error fetching from APIs, falling back to local storage", e)
            val cached = cacheHelper.getAllCachedRates()
            val copRateOffline = RateInfo(
                key = "COP",
                label = "Peso Colombiano",
                symbol = "COP",
                rate = if (cached.copPerUsd > 0.0) cached.usdBcv.rate / cached.copPerUsd else 0.0,
                dateUpdated = cached.copDate,
                isLocalOffline = true
            )
            TasaVeRatesResult.Success(
                usdBcv = cached.usdBcv.copy(isLocalOffline = true),
                eurBcv = cached.eurBcv.copy(isLocalOffline = true),
                usdtParalelo = cached.usdtParalelo.copy(isLocalOffline = true),
                nextUsdBcv = cached.nextUsdBcv?.copy(isLocalOffline = true),
                nextEurBcv = cached.nextEurBcv?.copy(isLocalOffline = true),
                nextBcvDateIso = cached.nextBcvDateIso,
                copPerUsd = cached.copPerUsd,
                copRate = copRateOffline,
                isFromCache = true,
                isOfflineError = true
            )
        }
    }

    suspend fun fetchHistoricalRates(forceRefresh: Boolean = false): List<RateSnapshot> = withContext(Dispatchers.IO) {
        val localSnaps = cacheHelper.getRateSnapshots()
        val cachedRemote = cacheHelper.getRemoteHistorySnapshots()

        if (!forceRefresh && !cacheHelper.isRemoteHistoryExpired() && cachedRemote.isNotEmpty()) {
            return@withContext mergeRemoteAndLocal(cachedRemote, localSnaps)
        }

        val remoteMerged = try {
            coroutineScope {
                val usdHistDeferred = async { runCatching { apiService.getHistoricalUsdOficial() }.getOrDefault(emptyList()) }
                val usdtHistDeferred = async { runCatching { apiService.getHistoricalUsdtParalelo() }.getOrDefault(emptyList()) }
                val eurHistDeferred = async { runCatching { apiService.getHistoricalEurOficial() }.getOrDefault(emptyList()) }

                val usdList = usdHistDeferred.await()
                val usdtList = usdtHistDeferred.await()
                val eurList = eurHistDeferred.await()

                buildDailySnapshots(usdList, eurList, usdtList)
            }
        } catch (e: Exception) {
            Log.e("TasaVeRepository", "Error fetching historical rates", e)
            emptyList()
        }

        if (remoteMerged.isNotEmpty()) {
            cacheHelper.saveRemoteHistorySnapshots(remoteMerged)
            mergeRemoteAndLocal(remoteMerged, localSnaps)
        } else {
            mergeRemoteAndLocal(cachedRemote, localSnaps)
        }
    }

    private fun buildDailySnapshots(
        usdList: List<HistoricalRateResponse>,
        eurList: List<HistoricalRateResponse>,
        usdtList: List<HistoricalRateResponse>
    ): List<RateSnapshot> {
        val usdByDay = usdList.mapNotNull { r ->
            val dayKey = extractDayKey(r.fecha)
            val rate = r.effectiveRate()
            if (dayKey != null && rate > 0.0) dayKey to rate else null
        }.toMap()

        val eurByDay = eurList.mapNotNull { r ->
            val dayKey = extractDayKey(r.fecha)
            val rate = r.effectiveRate()
            if (dayKey != null && rate > 0.0) dayKey to rate else null
        }.toMap()

        val usdtByDay = usdtList.mapNotNull { r ->
            val dayKey = extractDayKey(r.fecha)
            val rate = r.effectiveRate()
            if (dayKey != null && rate > 0.0) dayKey to rate else null
        }.toMap()

        val allDays = (usdByDay.keys + eurByDay.keys + usdtByDay.keys).toSortedSet()
        if (allDays.isEmpty()) return emptyList()

        val dayParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())

        var lastUsd = usdByDay.values.firstOrNull() ?: 0.0
        var lastEur = eurByDay.values.firstOrNull() ?: 0.0
        var lastUsdt = usdtByDay.values.firstOrNull() ?: 0.0

        return allDays.mapNotNull { dayKey ->
            usdByDay[dayKey]?.let { lastUsd = it }
            eurByDay[dayKey]?.let { lastEur = it }
            usdtByDay[dayKey]?.let { lastUsdt = it }

            val date = runCatching { dayParser.parse(dayKey) }.getOrNull() ?: return@mapNotNull null
            val ts = date.time + 12L * 60L * 60L * 1000L
            RateSnapshot(
                timestampMs = ts,
                label = labelFormatter.format(date),
                usdBcv = lastUsd,
                eurBcv = lastEur,
                usdt = lastUsdt
            )
        }
    }

    private fun mergeRemoteAndLocal(
        remote: List<RateSnapshot>,
        local: List<RateSnapshot>
    ): List<RateSnapshot> {
        val latestRemoteTs = remote.maxOfOrNull { it.timestampMs } ?: 0L
        val recentLocal = local.filter { it.timestampMs > latestRemoteTs - 12L * 60L * 60L * 1000L }
        return (remote + recentLocal).sortedBy { it.timestampMs }
    }

    private fun extractDayKey(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
            return trimmed.substring(0, 10)
        }
        return null
    }

    private fun formatDate(dateStr: String?): String? {
        if (dateStr == null) return null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val parsed = java.time.OffsetDateTime.parse(dateStr)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a", Locale.getDefault())
                return parsed.format(formatter)
            } catch (_: Exception) {
            }
        }

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )
        val outputFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val date = sdf.parse(dateStr)
                if (date != null) {
                    return outputFormat.format(date)
                }
            } catch (_: Exception) {
            }
        }
        return dateStr
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault()).format(Date())
    }

    companion object {
        fun todayCaracasIsoDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("America/Caracas")
            return sdf.format(Date())
        }

        fun formatIsoShortDate(iso: String): String {
            val parts = iso.split("-")
            return if (parts.size == 3) "${parts[2]}/${parts[1]}" else iso
        }

        fun resolveBcvTodayAndNext(
            bcvResp: DolarVzlaBcvResponse?,
            fallbackUsd: Double,
            fallbackEur: Double,
            todayIso: String = todayCaracasIsoDate()
        ): ResolvedBcvRates {
            val cur = bcvResp?.current
            val prev = bcvResp?.previous
            if (cur != null && cur.usd > 0.0 && cur.eur > 0.0) {
                val curDate = cur.date.trim()
                return if (curDate.isNotBlank() && curDate > todayIso) {
                    ResolvedBcvRates(
                        todayUsd = prev?.usd?.takeIf { it > 0.0 } ?: fallbackUsd,
                        todayEur = prev?.eur?.takeIf { it > 0.0 } ?: fallbackEur,
                        todayEffectiveIso = prev?.date?.takeIf { it.isNotBlank() } ?: todayIso,
                        nextUsd = cur.usd,
                        nextEur = cur.eur,
                        nextEffectiveIso = curDate,
                        usdChangePct = bcvResp.changePercentage?.usd,
                        eurChangePct = bcvResp.changePercentage?.eur
                    )
                } else {
                    ResolvedBcvRates(
                        todayUsd = cur.usd,
                        todayEur = cur.eur,
                        todayEffectiveIso = curDate.ifBlank { todayIso },
                        nextUsd = null,
                        nextEur = null,
                        nextEffectiveIso = null,
                        usdChangePct = bcvResp.changePercentage?.usd,
                        eurChangePct = bcvResp.changePercentage?.eur
                    )
                }
            }
            return ResolvedBcvRates(
                todayUsd = fallbackUsd,
                todayEur = fallbackEur,
                todayEffectiveIso = todayIso,
                nextUsd = null,
                nextEur = null,
                nextEffectiveIso = null,
                usdChangePct = null,
                eurChangePct = null
            )
        }
    }
}

sealed class TasaVeRatesResult {
    data class Success(
        val usdBcv: RateInfo,
        val eurBcv: RateInfo,
        val usdtParalelo: RateInfo,
        val nextUsdBcv: RateInfo? = null,
        val nextEurBcv: RateInfo? = null,
        val nextBcvDateIso: String? = null,
        val copPerUsd: Double = 4320.0,
        val copRate: RateInfo? = null,
        val isFromCache: Boolean,
        val isOfflineError: Boolean = false
    ) : TasaVeRatesResult()
    data class Error(val message: String) : TasaVeRatesResult()
}
