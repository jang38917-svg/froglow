package com.froglow.app.network.api

import com.froglow.app.network.model.LoginRequest
import com.froglow.app.network.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse
}