package com.froglow.app.ui.screens.calendar

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.froglow.app.network.CalendarDateItem
import com.froglow.app.network.DailyItem
import com.froglow.app.network.RetrofitClient
import com.froglow.app.network.RoutineItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CalendarUiState(
    val loadingDates: Boolean = false,
    val loadingDaily: Boolean = false,
    val markedDates: List<CalendarDateItem> = emptyList(),
    val selectedDate: String? = null,
    val dailyItems: List<DailyItem> = emptyList(),
    val dailyRoutineAM: List<RoutineItemDto> = emptyList(),
    val dailyRoutinePM: List<RoutineItemDto> = emptyList(),
    val error: String? = null
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.create(application)

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state

    fun loadCurrentMonth() {
        val today = LocalDate.now()
        loadMonth(today.year, today.monthValue)
    }

    fun loadMonth(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    loadingDates = true,
                    error = null
                )

                val response = apiService.getCalendarDates(year, month)

                _state.value = _state.value.copy(
                    loadingDates = false,
                    markedDates = response.dates,
                    error = null
                )

                Log.d("CALENDAR_DEBUG", "dates=${response.dates}")
            } catch (e: Exception) {
                Log.e("CALENDAR_DEBUG", "loadMonth error", e)
                _state.value = _state.value.copy(
                    loadingDates = false,
                    markedDates = emptyList(),
                    error = e.message ?: "캘린더 날짜 조회 실패"
                )
            }
        }
    }

    fun loadDaily(date: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    loadingDaily = true,
                    selectedDate = date,
                    dailyItems = emptyList(),
                    dailyRoutineAM = emptyList(),
                    dailyRoutinePM = emptyList(),
                    error = null
                )

                val response = apiService.getCalendarDaily(date)

                val hasDiagnosisRecord = response.items.isNotEmpty()

                _state.value = _state.value.copy(
                    loadingDaily = false,
                    selectedDate = date,
                    dailyItems = response.items,
                    dailyRoutineAM = if (hasDiagnosisRecord) {
                        response.routine?.AM?.sortedBy { it.step_order } ?: emptyList()
                    } else {
                        emptyList()
                    },
                    dailyRoutinePM = if (hasDiagnosisRecord) {
                        response.routine?.PM?.sortedBy { it.step_order } ?: emptyList()
                    } else {
                        emptyList()
                    },
                    error = null
                )

                Log.d("CALENDAR_DEBUG", "daily date=$date")
                Log.d("CALENDAR_DEBUG", "dailyItems count=${response.items.size}")
            } catch (e: Exception) {
                Log.e("CALENDAR_DEBUG", "loadDaily error", e)
                _state.value = _state.value.copy(
                    loadingDaily = false,
                    selectedDate = date,
                    dailyItems = emptyList(),
                    dailyRoutineAM = emptyList(),
                    dailyRoutinePM = emptyList(),
                    error = e.message ?: "일별 기록 조회 실패"
                )
            }
        }
    }
}