package com.froglow.app.network.model

data class LoginRequest(
    val login_id: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String
)