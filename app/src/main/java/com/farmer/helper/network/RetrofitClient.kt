package com.farmer.helper.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/") // base URL for weather API
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
