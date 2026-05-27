package com.froglow.app.ui.screens.main.products

data class ProductUi(
    val id: Long,
    val name: String,
    val subTag: String,
    val rating: Double,
    val imageUrl: String?,
    val buyUrl: String? = null,  // ✅ 추가

    // 🔥 상세페이지용 (0~10 점수)
    val acneScore: Int = 0,
    val rednessScore: Int = 0,
    val spotScore: Int = 0,
    val wrinkleScore: Int = 0
)