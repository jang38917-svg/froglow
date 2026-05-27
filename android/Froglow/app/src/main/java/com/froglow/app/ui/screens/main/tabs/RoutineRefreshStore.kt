package com.froglow.app.ui.screens.main.tabs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RoutineRefreshStore {

    private val _refreshKey = MutableStateFlow(0)
    val refreshKey = _refreshKey.asStateFlow()

    fun refresh() {
        _refreshKey.value = _refreshKey.value + 1
    }
}