package com.froglow.app.network

import android.content.Context
import android.util.Log
import com.froglow.app.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionManager = SessionManager(context)
        val token = sessionManager.getAccessToken()

        Log.d("TOKEN_DEBUG", "token = $token")
        Log.d("TOKEN_DEBUG", "url = ${chain.request().url}")

        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank() && !sessionManager.isTokenExpired(token)) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()

        Log.d("TOKEN_DEBUG", "authHeader = ${request.header("Authorization")}")

        return chain.proceed(request)
    }
}