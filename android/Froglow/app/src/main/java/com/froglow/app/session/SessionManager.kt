package com.froglow.app.session

import android.content.Context
import android.util.Base64
import org.json.JSONObject

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        val token = getAccessToken() ?: return false
        return !isTokenExpired(token)
    }

    fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true

            val payloadBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val payload = String(payloadBytes, Charsets.UTF_8)
            val json = JSONObject(payload)

            val exp = json.optLong("exp", 0L)
            if (exp == 0L) return true

            val nowSeconds = System.currentTimeMillis() / 1000
            nowSeconds >= exp
        } catch (e: Exception) {
            true
        }
    }
}