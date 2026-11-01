package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.ThemePreference

internal fun copyPortableTo(
    target: MutablePreferences,
    parsed: Preferences,
) {
    val merged = target.toMutablePreferences()
    writePortableValues(merged, parsed)
    target.write(merged.toAppSettings())
    target.writeThemeColors(merged.readThemeColors())
    val global = listOf("theme", "home_board", "favorite_boards_v2", "expanded_board_categories")
    global.forEach { name ->
        val key = stringPreferencesKey(name)
        parsed[key]?.let { value ->
            if (name == "theme") require(ThemePreference.entries.any { it.name == value })
            target[key] = value
        }
    }
}

/** Already validated fields only; unrelated stored values remain byte-for-byte unchanged. */
internal fun writePortableValues(
    target: MutablePreferences,
    parsed: Preferences,
) {
    if (parsed[stringPreferencesKey("theme")] != null) {
        if (parsed[stringPreferencesKey("theme_palette")] == null) {
            target.remove(stringPreferencesKey("theme_palette"))
        }
        if (parsed[
                androidx.datastore.preferences.core
                    .booleanPreferencesKey("theme_pure_black"),
            ] == null
        ) {
            target.remove(
                androidx.datastore.preferences.core
                    .booleanPreferencesKey("theme_pure_black"),
            )
        }
    }
    if (parsed[stringPreferencesKey("theme_palette")] != null &&
        parsed[
            androidx.datastore.preferences.core
                .intPreferencesKey("theme_custom_seed"),
        ] == null
    ) {
        target.remove(
            androidx.datastore.preferences.core
                .intPreferencesKey("theme_custom_seed"),
        )
    }
    val schema = portableSchema().asMap()
    parsed.asMap().forEach { (key, value) ->
        val type = schema.entries.single { it.key.name == key.name }.value
        when (type) {
            is Boolean -> {
                target[
                    androidx.datastore.preferences.core
                        .booleanPreferencesKey(key.name),
                ] = value as Boolean
            }

            is Int -> {
                target[
                    androidx.datastore.preferences.core
                        .intPreferencesKey(key.name),
                ] = value as Int
            }

            is String -> {
                target[stringPreferencesKey(key.name)] = value as String
            }

            is Set<*> -> {
                target[
                    androidx.datastore.preferences.core
                        .stringSetPreferencesKey(key.name),
                ] =
                    (value as Set<*>)
                        .map {
                            require(it is String)
                            it
                        }.toSet()
            }
        }
    }
}
