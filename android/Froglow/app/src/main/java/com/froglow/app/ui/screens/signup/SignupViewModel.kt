package com.froglow.app.ui.screens.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.froglow.app.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SignupUiState(
    val loginId: String = "",
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val loading: Boolean = false,
    val errorMessage: String = "",
    val success: Boolean = false
)

class SignupViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AuthRepository(getApplication())

    private val _state = MutableStateFlow(SignupUiState())
    val state: StateFlow<SignupUiState> = _state

    fun updateLoginId(v: String) {
        _state.value = _state.value.copy(loginId = v)
    }

    fun updateEmail(v: String) {
        _state.value = _state.value.copy(email = v)
    }

    fun updatePassword(v: String) {
        _state.value = _state.value.copy(password = v)
    }

    fun updateName(v: String) {
        _state.value = _state.value.copy(name = v)
    }

    fun updateGender(v: String) {
        _state.value = _state.value.copy(gender = v)
    }

    fun updateBirthDate(v: String) {
        _state.value = _state.value.copy(birthDate = v)
    }

    fun signup(onSuccess: () -> Unit = {}) {
        val current = _state.value

        viewModelScope.launch {
            _state.value = current.copy(
                loading = true,
                errorMessage = "",
                success = false
            )

            try {
                repo.register(
                    loginId = current.loginId,
                    email = current.email,
                    password = current.password,
                    name = current.name,
                    gender = current.gender.ifBlank { null },
                    birthDate = current.birthDate.ifBlank { null }
                )

                _state.value = _state.value.copy(
                    loading = false,
                    success = true
                )
                onSuccess()

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    errorMessage = e.message ?: "회원가입 실패"
                )
            }
        }
    }
}