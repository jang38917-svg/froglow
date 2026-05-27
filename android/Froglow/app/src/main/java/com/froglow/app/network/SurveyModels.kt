package com.froglow.app.network

data class SurveyOption(
    val id: Int,
    val text: String
)

data class SurveyQuestion(
    val id: Int,
    val text: String,
    val order: Int,
    val options: List<SurveyOption>
)

data class SurveyAnswerItem(
    val question_id: Int,
    val option_id: Int
)

/**
 * ✅ 백엔드 SurveySubmitIn에 맞춤
 * - preset_skin_type: "건성" / "지성" / "복합성" / "모르겠습니다"
 * - 모르겠습니다(or null)일 때만 answers를 채워서 전송
 */
data class SurveySubmitRequest(
    val preset_skin_type: String? = null,
    val answers: List<SurveyAnswerItem> = emptyList()
)

/**
 * ✅ 백엔드 SurveyResultOut에 맞춤
 * (응답/결과값을 서버 그대로 받기 위해 필드명 동일하게 구성)
 */
data class SurveySubmitResponse(
    val skin_type: String,
    val scores: Map<String, Int>,
    val response_id: Int
)
