package com.froglow.app.ui.screens.main.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.froglow.app.network.ApiService
import com.froglow.app.network.DiagnosisStatsResponse
import com.froglow.app.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StatsUiState(
    val isLoading: Boolean = false,
    val selectedMode: StatsMode = StatsMode.WEEKLY,
    val selectedDate: LocalDate = LocalDate.now(),
    val stats: DiagnosisStatsResponse? = null,
    val errorMessage: String? = null
)

enum class StatsMode {
    WEEKLY,
    MONTHLY
}

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService: ApiService =
        RetrofitClient.create(application.applicationContext)

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadWeeklyStats()
    }

    fun selectWeekly() {
        _uiState.value = _uiState.value.copy(selectedMode = StatsMode.WEEKLY)
        loadWeeklyStats(_uiState.value.selectedDate)
    }

    fun selectMonthly() {
        _uiState.value = _uiState.value.copy(selectedMode = StatsMode.MONTHLY)
        loadMonthlyStats(_uiState.value.selectedDate)
    }

    fun refresh() {
        val current = _uiState.value
        if (current.selectedMode == StatsMode.WEEKLY) {
            loadWeeklyStats(current.selectedDate)
        } else {
            loadMonthlyStats(current.selectedDate)
        }
    }

    fun movePrevious() {
        val current = _uiState.value
        val newDate = if (current.selectedMode == StatsMode.WEEKLY) {
            current.selectedDate.minusMonths(1)
        } else {
            current.selectedDate.minusYears(1)
        }

        _uiState.value = current.copy(selectedDate = newDate)
        refresh()
    }

    fun moveNext() {
        val current = _uiState.value
        val newDate = if (current.selectedMode == StatsMode.WEEKLY) {
            current.selectedDate.plusMonths(1)
        } else {
            current.selectedDate.plusYears(1)
        }

        _uiState.value = current.copy(selectedDate = newDate)
        refresh()
    }

    private fun loadWeeklyStats(date: LocalDate = _uiState.value.selectedDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val response = apiService.getWeeklyStats(
                    year = date.year,
                    month = date.monthValue
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stats = response,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stats = null,
                    errorMessage = e.message ?: "주간 통계 조회 실패"
                )
            }
        }
    }

    private fun loadMonthlyStats(date: LocalDate = _uiState.value.selectedDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val response = apiService.getMonthlyStats(
                    year = date.year
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stats = response,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stats = null,
                    errorMessage = e.message ?: "월간 통계 조회 실패"
                )
            }
        }
    }
}