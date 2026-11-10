package com.julcer26.tasave.data

import retrofit2.http.GET

interface DolarApiService {
    @GET("v1/dolares/oficial")
    suspend fun getUsdOficial(): ExchangeRateResponse

    @GET("v1/dolares/paralelo")
    suspend fun getUsdtParalelo(): ExchangeRateResponse

    @GET("v1/euros/oficial")
    suspend fun getEurOficial(): ExchangeRateResponse
}
