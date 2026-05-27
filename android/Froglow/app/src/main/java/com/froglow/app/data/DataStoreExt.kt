package com.froglow.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// ✅ 표준 delegate 방식 (this 문제 없음)
private val Context._sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session_store"
)

val Context.sessionDataStore: DataStore<Preferences>
    get() = _sessionDataStore
