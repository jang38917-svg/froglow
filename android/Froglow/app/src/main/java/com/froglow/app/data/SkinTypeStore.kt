package com.froglow.app.data

import android.content.Context

object SkinTypeStore {

    private const val PREF_NAME = "froglow_skin_pref"
    private const val KEY_SKIN_TYPE = "skin_type"

    fun saveSkinType(context: Context, skinType: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SKIN_TYPE, skinType)
            .apply()
    }

    fun getSkinType(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SKIN_TYPE, null)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SKIN_TYPE)
            .apply()
    }
}