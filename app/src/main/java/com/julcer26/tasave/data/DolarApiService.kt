package com.julcer26.tasave.data

import retrofit2.http.GET
import retrofit2.http.Url

interface DolarApiService {
    @GET("v1/dolares/oficial")
    suspend fun getUsdOficial(): ExchangeRateResponse

    @GET("v1/dolares/paralelo")
    suspend fun getUsdtParalelo(): ExchangeRateResponse

    @GET("v1/euros/oficial")
    suspend fun getEurOficial(): ExchangeRateResponse

    @GET("v1/historicos/dolares/oficial")
    suspend fun getHistoricalUsdOficial(): List<HistoricalRateResponse>

    @GET("v1/historicos/dolares/paralelo")
    suspend fun getHistoricalUsdtParalelo(): List<HistoricalRateResponse>

    @GET("v1/historicos/euros/oficial")
    suspend fun getHistoricalEurOficial(): List<HistoricalRateResponse>

    @GET
    suspend fun getBcvDetailed(@Url url: String = "https://rates.dolarvzla.com/bcv/current.json"): DolarVzlaBcvResponse
}
