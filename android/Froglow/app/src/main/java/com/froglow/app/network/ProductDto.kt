package com.froglow.app.network

data class ProductDto(
    val product_id: Long,
    val product_name: String,
    val product_image_url: String?,
    val acne_grade: String?,
    val redness_grade: String?,
    val wrinkle_grade: String?,
    val spot_grade: String?,
    val recommend_score: Double?,
    val is_active: Boolean?,
    val formulation: String?,
    val concerns: String?
)

data class ReminderDto(
    val id: Long,
    val user_id: Long,
    val reminder_type: String,
    val routine_key: String?,
    val frequency: String,
    val interval_hours: Int?,
    val remind_time: String?,
    val timezone: String?,
    val label: String?,
    val is_active: Boolean
)

data class ReminderRequest(
    val reminder_type: String,
    val routine_key: String?,
    val frequency: String,
    val interval_hours: Int?,
    val remind_time: String,
    val timezone: String = "Asia/Seoul",
    val label: String,
    val is_active: Boolean = true
)

data class ReminderUpdateRequest(
    val reminder_type: String? = null,
    val routine_key: String? = null,
    val frequency: String? = null,
    val interval_hours: Int? = null,
    val remind_time: String? = null,
    val timezone: String? = null,
    val label: String? = null,
    val is_active: Boolean? = null
)

data class DeleteReminderResponse(
    val ok: Boolean
)

data class ChatRequest(
    val message: String
)

data class RoutineCompletionResponse(
    val date: String,
    val AM: Boolean,
    val PM: Boolean
)

data class RoutineCompletionRequest(
    val date: String,
    val routine_type: String,
    val is_completed: Boolean
)

data class RoutineCompletionSaveResponse(
    val date: String,
    val routine_type: String,
    val is_completed: Boolean,
    val completed_at: String?
)