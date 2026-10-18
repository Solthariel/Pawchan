package com.solthariel.pawchan.archive

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Копия настройки «Обновлять сохранённые копии» для фоновой проверки, которая работает без DataStore UI. */
internal object ArchiveUpdatePreference {
    fun set(
        context: Context,
        enabled: Boolean,
    ) {
        preferences(context).edit { putBoolean(KEY, enabled) }
    }

    fun enabled(context: Context): Boolean = preferences(context).getBoolean(KEY, true)

    private fun preferences(context: Context): SharedPreferences {
        val app = context.applicationContext
        return app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private const val KEY = "update_saved_copies"
    private const val PREFERENCES_NAME = "archive_updates"
}
