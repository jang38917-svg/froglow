package com.froglow.app.ui.screens.main.tabs

import android.content.Context
import java.time.LocalDate

object RoutineProgressStore {
    private const val PREF_NAME = "routine_progress_pref"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun todayKey(type: String): String {
        return "routine_completed_${LocalDate.now()}_$type"
    }

    fun getAmCompletedCount(context: Context): Int {
        return prefs(context).getInt(todayKey("AM"), 0)
    }

    fun getPmCompletedCount(context: Context): Int {
        return prefs(context).getInt(todayKey("PM"), 0)
    }

    fun setAmCompletedCount(context: Context, value: Int) {
        prefs(context).edit()
            .putInt(todayKey("AM"), value)
            .apply()
    }

    fun setPmCompletedCount(context: Context, value: Int) {
        prefs(context).edit()
            .putInt(todayKey("PM"), value)
            .apply()
    }

    fun clearToday(context: Context) {
        prefs(context).edit()
            .remove(todayKey("AM"))
            .remove(todayKey("PM"))
            .apply()
    }
}