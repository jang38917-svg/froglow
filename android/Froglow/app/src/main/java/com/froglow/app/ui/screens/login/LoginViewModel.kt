package com.froglow.app.ui.screens.login

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.froglow.app.network.LoginRequest
import com.froglow.app.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class LoginUiState(
    val loginId: String = "",
    val password: String = "",
    val errorMessage: String = ""
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    val state = mutableStateOf(LoginUiState())

    fun updateLoginId(v: String) {
        state.value = state.value.copy(loginId = v)
    }

    fun updatePassword(v: String) {
        state.value = state.value.copy(password = v)
    }

    fun login(onSuccess: (needsSurvey: Boolean, accessToken: String) -> Unit) {
        val loginId = state.value.loginId
        val password = state.value.password

        viewModelScope.launch {
            try {
                val api = RetrofitClient.create(getApplication())
                val res = api.login(
                    LoginRequest(
                        login_id = loginId,
                        password = password
                    )
                )

                // 토큰 저장
                val prefs = getApplication<Application>()
                    .getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

                prefs.edit()
                    .putString("access_token", res.access_token)
                    .putString("login_id", loginId)
                    .apply()

                state.value = state.value.copy(errorMessage = "")
                onSuccess(res.needs_survey, res.access_token)

            } catch (e: HttpException) {
                val status = e.code()
                state.value = state.value.copy(
                    errorMessage = "서버 오류: $status"
                )

            } catch (e: IOException) {
                state.value = state.value.copy(
                    errorMessage = "네트워크 오류: 인터넷 연결 확인"
                )

            } catch (e: Exception) {
                state.value = state.value.copy(
                    errorMessage = "알 수 없는 오류: ${e.message ?: "unknown"}"
                )
            }
        }
    }
}