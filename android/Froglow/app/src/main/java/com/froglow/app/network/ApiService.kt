package com.froglow.app.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @GET("survey/questions")
    suspend fun getSurveyQuestions(): List<SurveyQuestion>

    @GET("survey/preset-options")
    suspend fun getPresetOptions(): List<String>

    @POST("survey/submit")
    suspend fun submitSurvey(
        @Body request: SurveySubmitRequest
    ): SurveySubmitResponse

    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("products/recommend")
    suspend fun getRecommendedProducts(
        @Query("skin_type") skinType: String
    ): List<ProductDto>

    @POST("api/v1/chatbot/ask")
    suspend fun askChatbot(
        @Body request: ChatRequest
    ): ChatResponse

    @Multipart
    @POST("api/v1/analyze")
    suspend fun analyzeSkin(
        @Part front: MultipartBody.Part,
        @Part left: MultipartBody.Part,
        @Part right: MultipartBody.Part,
        @Part("record_date") recordDate: RequestBody
    ): DiagnosisAnalyzeResponse

    @Multipart
    @POST("api/v1/analyze/preview")
    suspend fun analyzeSkinPreview(
        @Part front: MultipartBody.Part,
        @Part left: MultipartBody.Part,
        @Part right: MultipartBody.Part
    ): DiagnosisAnalyzeResponse

    @GET("api/v1/diagnosis/calendar/dates")
    suspend fun getCalendarDates(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): CalendarDatesResponse

    @GET("api/v1/diagnosis/calendar/daily")
    suspend fun getCalendarDaily(
        @Query("date") date: String
    ): CalendarDailyResponse

    @GET("routines")
    suspend fun getRoutine(
        @Query("routine_type") routineType: String
    ): RoutineResponse

    @POST("routines")
    suspend fun saveRoutine(
        @Body request: RoutineRequest
    ): RoutineResponse

    @GET("api/v1/diagnosis/stats/weekly")
    suspend fun getWeeklyStats(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): DiagnosisStatsResponse

    @GET("api/v1/diagnosis/stats/monthly")
    suspend fun getMonthlyStats(
        @Query("year") year: Int
    ): DiagnosisStatsResponse

    @GET("reminders")
    suspend fun getReminders(): List<ReminderDto>

    @POST("reminders")
    suspend fun createReminder(
        @Body request: ReminderRequest
    ): ReminderDto

    @PATCH("reminders/{reminder_id}")
    suspend fun updateReminder(
        @Path("reminder_id") reminderId: Long,
        @Body request: ReminderUpdateRequest
    ): ReminderDto

    @DELETE("reminders/{reminder_id}")
    suspend fun deleteReminder(
        @Path("reminder_id") reminderId: Long
    ): DeleteReminderResponse

    @GET("routines/completion")
    suspend fun getRoutineCompletion(
        @Query("date") date: String
    ): RoutineCompletionResponse

    @POST("routines/completion")
    suspend fun saveRoutineCompletion(
        @Body request: RoutineCompletionRequest
    ): RoutineCompletionSaveResponse

    @FormUrlEncoded
    @POST("api/v1/save")
    suspend fun saveDiagnosis(
        @Field("folder_id") folderId: String,
        @Field("acne") acne: Int,
        @Field("redness") redness: Int,
        @Field("wrinkle") wrinkle: Int,
        @Field("spot") spot: Int,
        @Field("primary_concern") primaryConcern: String?,
        @Field("skin_type") skinType: String?,
        @Field("record_date") recordDate: String?
    ): DiagnosisAnalyzeResponse
}