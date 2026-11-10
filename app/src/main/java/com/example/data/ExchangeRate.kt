package com.example.data

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

data class RateInfo(
    val key: String, // "USD_BCV", "EUR_BCV", "USDT"
    val label: String, // "Dólar BCV", "Euro BCV", "USDT Paralelo"
    val symbol: String, // "$", "€", "₮"
    val rate: Double,
    val dateUpdated: String,
    val isLocalOffline: Boolean = false
)
