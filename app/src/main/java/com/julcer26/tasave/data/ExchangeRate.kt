package com.julcer26.tasave.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeRateResponse(
    @param:Json(name = "moneda") val moneda: String,
    @param:Json(name = "nombre") val nombre: String,
    @param:Json(name = "compra") val compra: Double? = null,
    @param:Json(name = "venta") val venta: Double? = null,
    @param:Json(name = "promedio") val promedio: Double,
    @param:Json(name = "fechaActualizacion") val fechaActualizacion: String? = null
)

@JsonClass(generateAdapter = true)
data class HistoricalRateResponse(
    @param:Json(name = "fuente") val fuente: String? = null,
    @param:Json(name = "compra") val compra: Double? = null,
    @param:Json(name = "venta") val venta: Double? = null,
    @param:Json(name = "promedio") val promedio: Double? = null,
    @param:Json(name = "fecha") val fecha: String = ""
) {
    fun effectiveRate(): Double = promedio ?: venta ?: compra ?: 0.0
}

@JsonClass(generateAdapter = true)
data class DolarVzlaBcvSnapshot(
    @param:Json(name = "date") val date: String = "",
    @param:Json(name = "usd") val usd: Double = 0.0,
    @param:Json(name = "eur") val eur: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DolarVzlaChangePct(
    @param:Json(name = "usd") val usd: Double? = null,
    @param:Json(name = "eur") val eur: Double? = null
)

@JsonClass(generateAdapter = true)
data class DolarVzlaBcvResponse(
    @param:Json(name = "current") val current: DolarVzlaBcvSnapshot? = null,
    @param:Json(name = "previous") val previous: DolarVzlaBcvSnapshot? = null,
    @param:Json(name = "changePercentage") val changePercentage: DolarVzlaChangePct? = null
)

@JsonClass(generateAdapter = true)
data class ColombiaTrmResponse(
    @param:Json(name = "valor") val valor: Double? = null,
    @param:Json(name = "nombre") val nombre: String? = null,
    @param:Json(name = "unidad") val unidad: String? = null,
    @param:Json(name = "fechaActualizacion") val fechaActualizacion: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenErUsdResponse(
    @param:Json(name = "result") val result: String? = null,
    @param:Json(name = "rates") val rates: Map<String, Double>? = null
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
