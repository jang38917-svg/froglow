package com.froglow.app.ui.screens.survey

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.froglow.app.data.SkinTypeStore
import com.froglow.app.network.RetrofitClient
import com.froglow.app.network.SurveyAnswerItem
import com.froglow.app.network.SurveyQuestion
import com.froglow.app.network.SurveySubmitRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SurveyUiState(
    val loading: Boolean = false,
    val questions: List<SurveyQuestion> = emptyList(),
    val selectedOptionIds: Map<Int, Int> = emptyMap(),
    val submitSuccess: Boolean = false,
    val navigateToCamera: Boolean = false,
    val error: String? = null
)

class SurveyViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.create(application)

    private val _state = MutableStateFlow(SurveyUiState())
    val state: StateFlow<SurveyUiState> = _state

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    loading = true,
                    error = null
                )

                val response = apiService.getSurveyQuestions()

                _state.value = _state.value.copy(
                    loading = false,
                    questions = response,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("SurveyViewModel", "loadQuestions error", e)
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "설문 문항을 불러오지 못했습니다."
                )
            }
        }
    }

    fun selectOption(questionId: Int, optionId: Int) {
        val updated = _state.value.selectedOptionIds.toMutableMap()
        updated[questionId] = optionId

        _state.value = _state.value.copy(
            selectedOptionIds = updated
        )
    }

    fun submitSurvey() {
        Log.d("SURVEY_DEBUG", "2. submitSurvey 시작")

        val current = _state.value

        viewModelScope.launch {
            try {
                Log.d("SURVEY_DEBUG", "3. API 호출 직전")

                _state.value = _state.value.copy(
                    loading = true,
                    error = null,
                    submitSuccess = false,
                    navigateToCamera = false
                )

                val answers = current.selectedOptionIds.map { (questionId, optionId) ->
                    SurveyAnswerItem(
                        question_id = questionId,
                        option_id = optionId
                    )
                }

                val request = SurveySubmitRequest(answers = answers)
                val response = apiService.submitSurvey(request)

                Log.d("SURVEY_DEBUG", "4. API 성공 = $response")

                val normalizedSkinType = normalizeSkinType(response.skin_type)

                SkinTypeStore.saveSkinType(
                    getApplication(),
                    normalizedSkinType
                )

                Log.d("SURVEY_DEBUG", "saved skinType = $normalizedSkinType")

                _state.value = _state.value.copy(
                    loading = false,
                    submitSuccess = true,
                    navigateToCamera = true,
                    error = null
                )

                Log.d("SURVEY_DEBUG", "5. navigateToCamera=true 설정 완료")

            } catch (e: Exception) {
                Log.e("SURVEY_DEBUG", "ERROR. 설문 제출 실패", e)

                _state.value = _state.value.copy(
                    loading = false,
                    submitSuccess = false,
                    navigateToCamera = false,
                    error = e.message ?: "설문 제출 실패"
                )
            }
        }
    }

    private fun normalizeSkinType(value: String?): String {
        return when (value?.trim()?.uppercase()) {
            "DRY" -> "DRY"
            "OILY" -> "OILY"
            "COMB", "COMBINATION" -> "COMB"
            else -> "OILY"
        }
    }

    fun onCameraNavigationHandled() {
        _state.value = _state.value.copy(
            navigateToCamera = false
        )
    }
}