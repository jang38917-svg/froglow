package com.froglow.app.data

import android.content.Context
import com.froglow.app.network.LoginRequest
import com.froglow.app.network.LoginResponse
import com.froglow.app.network.RegisterRequest
import com.froglow.app.network.RegisterResponse
import com.froglow.app.network.RetrofitClient

class AuthRepository(
    private val context: Context
) {
    suspend fun login(loginId: String, password: String): LoginResponse {
        return RetrofitClient.create(context).login(
            LoginRequest(
                login_id = loginId,
                password = password
            )
        )
    }

    suspend fun register(
        loginId: String,
        email: String,
        password: String,
        name: String,
        gender: String?,
        birthDate: String?
    ): RegisterResponse {
        return RetrofitClient.create(context).register(
            RegisterRequest(
                login_id = loginId,
                email = email,
                password = password,
                name = name,
                gender = gender,
                birth_date = birthDate
            )
        )
    }
}