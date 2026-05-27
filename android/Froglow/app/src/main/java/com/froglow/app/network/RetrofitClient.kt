package com.froglow.app.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val BASE_URL = "http://192.168.0.16:8000/"

    fun create(context: Context): ApiService {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(logger)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun buildImageUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }

        val normalizedBaseUrl = BASE_URL.removeSuffix("/")
        return if (path.startsWith("/")) {
            "$normalizedBaseUrl$path"
        } else {
            "$normalizedBaseUrl/$path"
        }
    }
}