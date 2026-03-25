package com.closetai.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // For local testing on Android emulator hitting local Python FastAPI
    // 10.0.2.2 is the special alias to the host loopback interface (localhost) on the emulator
    // Backend reachable over the same Wi-Fi network.
    // PC Wi-Fi IPv4 (from `ipconfig`) is expected to be 192.168.31.179.
    private const val BASE_URL = "http://192.168.31.179:8081/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS) // Scraping might take some time
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val closetAiApi: ClosetAiApi by lazy {
        retrofit.create(ClosetAiApi::class.java)
    }
}
