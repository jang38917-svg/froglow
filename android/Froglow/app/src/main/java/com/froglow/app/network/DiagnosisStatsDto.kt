package com.froglow.app.network

import com.google.gson.annotations.SerializedName

data class DiagnosisStatsResponse(
    @SerializedName("period")
    val period: String,

    @SerializedName("year")
    val year: Int? = null,

    @SerializedName("month")
    val month: Int? = null,

    @SerializedName("start_date")
    val startDate: String? = null,

    @SerializedName("end_date")
    val endDate: String? = null,

    @SerializedName("average")
    val average: DiagnosisAverageDto? = null,

    @SerializedName("items")
    val items: List<DiagnosisStatsItemDto> = emptyList()
)

data class DiagnosisAverageDto(
    @SerializedName("acne")
    val acne: Float? = null,

    @SerializedName("redness")
    val redness: Float? = null,

    @SerializedName("wrinkle")
    val wrinkle: Float? = null,

    @SerializedName("spot")
    val spot: Float? = null
)

data class DiagnosisStatsItemDto(
    @SerializedName("week")
    val week: Int? = null,

    @SerializedName("month")
    val month: Int? = null,

    @SerializedName("label")
    val label: String,

    @SerializedName("start_date")
    val startDate: String? = null,

    @SerializedName("end_date")
    val endDate: String? = null,

    @SerializedName("acne")
    val acne: Float? = null,

    @SerializedName("redness")
    val redness: Float? = null,

    @SerializedName("wrinkle")
    val wrinkle: Float? = null,

    @SerializedName("spot")
    val spot: Float? = null
)