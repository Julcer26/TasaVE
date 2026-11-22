package com.julcer26.tasave.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeRateResponse(
    @Json(name = "moneda") val moneda: String,
    @Json(name = "nombre") val nombre: String,
    @Json(name = "compra") val compra: Double? = null,
    @Json(name = "venta") val venta: Double? = null,
    @Json(name = "promedio") val promedio: Double,
    @Json(name = "fechaActualizacion") val fechaActualizacion: String? = null
)

@JsonClass(generateAdapter = true)
data class HistoricalRateResponse(
    @Json(name = "fuente") val fuente: String? = null,
    @Json(name = "compra") val compra: Double? = null,
    @Json(name = "venta") val venta: Double? = null,
    @Json(name = "promedio") val promedio: Double? = null,
    @Json(name = "fecha") val fecha: String = ""
) {
    fun effectiveRate(): Double = promedio ?: venta ?: compra ?: 0.0
}

@JsonClass(generateAdapter = true)
data class DolarVzlaBcvSnapshot(
    @Json(name = "date") val date: String = "",
    @Json(name = "usd") val usd: Double = 0.0,
    @Json(name = "eur") val eur: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DolarVzlaChangePct(
    @Json(name = "usd") val usd: Double? = null,
    @Json(name = "eur") val eur: Double? = null
)

@JsonClass(generateAdapter = true)
data class DolarVzlaBcvResponse(
    @Json(name = "current") val current: DolarVzlaBcvSnapshot? = null,
    @Json(name = "previous") val previous: DolarVzlaBcvSnapshot? = null,
    @Json(name = "changePercentage") val changePercentage: DolarVzlaChangePct? = null
)

@JsonClass(generateAdapter = true)
data class ColombiaTrmResponse(
    @Json(name = "valor") val valor: Double? = null,
    @Json(name = "nombre") val nombre: String? = null,
    @Json(name = "unidad") val unidad: String? = null,
    @Json(name = "fechaActualizacion") val fechaActualizacion: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenErUsdResponse(
    @Json(name = "result") val result: String? = null,
    @Json(name = "rates") val rates: Map<String, Double>? = null
)

data class RateInfo(
    val key: String, // "USD_BCV", "EUR_BCV", "USDT", "COP"
    val label: String, // "Dólar BCV", "Euro BCV", "USDT Binance", "Peso Colombiano"
    val symbol: String, // "$", "€", "₮", "COP"
    val rate: Double,
    val dateUpdated: String,
    val isLocalOffline: Boolean = false,
    val effectiveDateIso: String? = null,
    val changePercent: Double? = null
)
