package com.froglow.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionStore(
    context: Context
) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(
        prefs.getBoolean("is_logged_in", false)
    )
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isSurveyDone = MutableStateFlow(
        prefs.getBoolean("is_survey_done", false)
    )
    val isSurveyDone: StateFlow<Boolean> = _isSurveyDone

    private val _accessToken = MutableStateFlow(
        prefs.getString("access_token", null)
    )
    val accessToken: StateFlow<String?> = _accessToken

    private val _skinType = MutableStateFlow(
        prefs.getString("skin_type", null)
    )
    val skinType: StateFlow<String?> = _skinType

    fun setLoggedIn(value: Boolean) {
        _isLoggedIn.value = value
        prefs.edit().putBoolean("is_logged_in", value).apply()
    }

    fun setSurveyDone(value: Boolean) {
        _isSurveyDone.value = value
        prefs.edit().putBoolean("is_survey_done", value).apply()
    }

    fun setAccessToken(token: String?) {
        _accessToken.value = token
        prefs.edit().putString("access_token", token).apply()
    }

    fun setSkinType(value: String?) {
        _skinType.value = value
        prefs.edit().putString("skin_type", value).apply()
    }

    suspend fun logout() {
        _isLoggedIn.value = false
        _isSurveyDone.value = false
        _accessToken.value = null
        _skinType.value = null
        prefs.edit().clear().apply()
    }
}