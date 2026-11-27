package com.solthariel.pawchan.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** Android owns the choice from API 33; older devices restore the same app-only preference. */
internal object PawchanLocales {
    val tags = listOf("ru", "en", "de", "pt-BR", "es", "fr", "it", "pl", "fi", "ja")
    private const val STORE = "pawchan_interface_locale"
    private const val KEY = "selected"

    fun selected(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = requireNotNull(context.getSystemService(LocaleManager::class.java))
            val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            if (prefs.contains(KEY)) {
                val old = prefs.getString(KEY, "").orEmpty()
                if (manager.applicationLocales.isEmpty && old in tags) {
                    manager.applicationLocales = LocaleList.forLanguageTags(old)
                }
                prefs.edit { remove(KEY) }
            }
            manager.applicationLocales.toLanguageTags().substringBefore(',')
        } else {
            context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
        }

    fun choose(
        activity: Activity,
        tag: String,
    ) {
        require(tag.isEmpty() || tag in tags)
        if (selected(activity) == tag) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = requireNotNull(activity.getSystemService(LocaleManager::class.java))
            manager.applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit { putString(KEY, tag) }
            activity.recreate()
        }
    }

    fun attach(context: Context): Context {
        val choice = selected(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context else legacyContext(context, choice)
    }
}

/** Before Android 13 the app applies the saved tag to this base only. Never change the process or device locale. */
internal fun legacyContext(
    base: Context,
    tag: String,
): Context {
    if (tag.isEmpty()) return base
    val config = Configuration(base.resources.configuration)
    config.setLocale(Locale.forLanguageTag(tag))
    return base.createConfigurationContext(config)
}
