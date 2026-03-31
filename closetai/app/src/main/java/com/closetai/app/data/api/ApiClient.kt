package com.closetai.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8081/"

    @Volatile
    private var baseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var retrofitInstance: Retrofit? = null

    @Volatile
    private var apiInstance: ClosetAiApi? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS) // Scraping might take some time
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun configureBaseUrl(newBaseUrl: String?) {
        val normalized = normalizeBaseUrl(newBaseUrl)
        synchronized(this) {
            if (normalized == baseUrl && retrofitInstance != null && apiInstance != null) return
            baseUrl = normalized
            retrofitInstance = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiInstance = retrofitInstance!!.create(ClosetAiApi::class.java)
        }
    }

    val retrofit: Retrofit
        get() {
            retrofitInstance?.let { return it }
            synchronized(this) {
                retrofitInstance?.let { return it }
                configureBaseUrl(baseUrl)
                return retrofitInstance!!
            }
        }

    val closetAiApi: ClosetAiApi
        get() {
            apiInstance?.let { return it }
            synchronized(this) {
                apiInstance?.let { return it }
                configureBaseUrl(baseUrl)
                return apiInstance!!
            }
        }

    private fun normalizeBaseUrl(input: String?): String {
        val trimmed = input?.trim().orEmpty()
        val candidate = if (trimmed.isBlank()) DEFAULT_BASE_URL else trimmed
        val withScheme = if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            candidate
        } else {
            "http://$candidate"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}
