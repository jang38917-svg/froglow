package com.froglow.app.network

import com.google.gson.annotations.SerializedName

// ---------- LOGIN ----------
data class LoginRequest(
    val login_id: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val access_token: String,
    val token_type: String,
    val needs_survey: Boolean,
    val login_id: String,
    val email: String,
    val name: String?
)

// ---------- REGISTER ----------
data class RegisterRequest(
    val login_id: String,
    val email: String,
    val password: String,
    val name: String,
    val gender: String? = null,
    val birth_date: String? = null
)

data class RegisterResponse(
    val message: String,
    val id: Int,
    val login_id: String,
    val email: String,
    val name: String
)

// ---------- CHATBOT ----------
data class ChatResponse(
    val reply: String
)

// ---------- CALENDAR ----------
data class CalendarDateItem(
    val date: String,
    val count: Int
)

data class CalendarDatesResponse(
    val year: Int,
    val month: Int,
    val dates: List<CalendarDateItem>
)

data class DiagnosisImages(
    val front: ImageItem?,
    val left: ImageItem?,
    val right: ImageItem?
)

data class ImageItem(
    val original: String? = null,
    val annotated: String? = null
)

data class DiagnosisData(
    val acne: Int,
    val redness: Int,
    val wrinkle: Int,
    val spot: Int
)

data class DailyItem(
    val id: Int,
    val primary_concern: String,
    val images: DiagnosisImages,
    val diagnosis: DiagnosisData
)

data class CalendarDailyResponse(
    val date: String,
    val items: List<DailyItem>,
    val routine: DailyRoutineBundle? = null,
    val routine_completion: RoutineCompletionDto? = null
)

data class DailyRoutineBundle(
    val AM: List<RoutineItemDto> = emptyList(),
    val PM: List<RoutineItemDto> = emptyList()
)

// ---------- ROUTINE ----------
data class RoutineRequest(
    val routine_type: String,
    val product_ids: List<Long>
)

data class RoutineItemDto(
    @SerializedName(value = "product_id", alternate = ["productId"])
    val product_id: Long = 0L,

    @SerializedName(value = "step_order", alternate = ["stepOrder"])
    val step_order: Int = 0,

    @SerializedName(value = "reason", alternate = ["Reason"])
    val reason: String? = null,

    @SerializedName(value = "product_name", alternate = ["productName"])
    val product_name: String? = null,

    @SerializedName(value = "step_name", alternate = ["stepName"])
    val step_name: String? = null,

    @SerializedName(value = "product_image_url", alternate = ["productImageUrl"])
    val product_image_url: String? = null
)

data class RoutineResponse(
    val id: Int,
    val user_id: Int,
    val routine_type: String,
    val items: List<RoutineItemDto>
)

// ---------- DIAGNOSIS ANALYZE ----------
data class DiagnosisAnalyzeResponse(
    val status: String,
    val folder_id: String? = null,
    val diagnosis: DiagnosisData,
    val primary_concern: String,
    val images: DiagnosisAnalyzeImages? = null,
    val annotated_images: DiagnosisAnalyzeImages? = null,
    val recommendation: RecommendationDto? = null
)

data class DiagnosisAnalyzeImages(
    val front: String? = null,
    val left: String? = null,
    val right: String? = null
)

data class RecommendationDto(
    val morning_routine: List<RoutineStepDto> = emptyList(),
    val evening_routine: List<RoutineStepDto> = emptyList()
)

data class RoutineStepDto(
    val step: String? = null,
    val step_name: String? = null,
    val description: String? = null,
    val items: List<RoutineProductDto> = emptyList()
)

data class RoutineProductDto(
    val product_id: Long,
    val product_name: String,
    val product_image_url: String? = null,
    val price: String? = null,
    val formulation: String? = null,
    val recommend_score: Int? = null,
    val acne_grade: String? = null,
    val spot_grade: String? = null,
    val redness_grade: String? = null,
    val wrinkle_grade: String? = null
)

// ---------- ROUTINE COMPLETION ----------

data class RoutineCompletionDto(
    val AM: Boolean,
    val PM: Boolean
)

data class DiagnosisSaveRequest(
    val folder_id: String,
    val diagnosis: DiagnosisData,
    val primary_concern: String,
    val record_date: String
)