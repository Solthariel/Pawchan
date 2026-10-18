package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.boards.FAVORITES_BOARD_CATEGORY_KEY
import com.solthariel.pawchan.domain.settings.AppSettings
import java.util.Base64

internal fun portableSchema(): Preferences =
    mutablePreferencesOf().apply {
        write(AppSettings())

        this[stringPreferencesKey("theme")] = "SYSTEM"
        this[stringPreferencesKey("home_board")] = ""
        this[stringPreferencesKey("favorite_boards_v2")] = ""
        this[stringPreferencesKey("expanded_board_categories")] = FAVORITES_BOARD_CATEGORY_KEY
    }

internal fun exportPortablePreferences(source: Preferences): Map<String, String> {
    val allowed =
        portableSchema()
            .asMap()
            .keys
            .map { it.name }
            .toSet()
    val complete =
        portableSchema()
            .asMap()
            .map { (key, default) ->
                key to (
                    source
                        .asMap()
                        .entries
                        .firstOrNull { it.key.name == key.name }
                        ?.value
                        ?: if (key.name == "favorite_boards_v2") {
                            source[stringSetPreferencesKey("favorite_boards")]?.sorted()?.joinToString("\n") ?: default
                        } else {
                            default
                        }
                )
            }.toMap()
    return complete
        .filterKeys { it.name in allowed }
        .map { (key, value) -> key.name to encodePortableValue(value) }
        .toMap()
}

internal fun validatePortablePreferences(source: Map<String, String>): Preferences {
    val schema = portableSchema().asMap()
    val result = mutablePreferencesOf()
    source.forEach { (name, encoded) ->
        val field =
            schema.entries.singleOrNull { it.key.name == name }
                ?: throw IllegalArgumentException("Неизвестная настройка: $name")
        copyPortableValue(result, field.key, field.value, encoded)
    }
    return result
}

private fun encodePortableValue(value: Any): String =
    when (value) {
        is Set<*> -> {
            value
                .map {
                    require(it is String)
                    it
                }.sorted()
                .joinToString(",") {
                    Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8))
                }
        }

        else -> {
            value.toString()
        }
    }

private fun copyPortableValue(
    result: MutablePreferences,
    key: Preferences.Key<*>,
    sample: Any,
    source: String,
) {
    when (sample) {
        is Boolean -> {
            result[
                androidx.datastore.preferences.core
                    .booleanPreferencesKey(key.name),
            ] =
                requireNotNull(source.toBooleanStrictOrNull())
        }

        is Int -> {
            result[
                androidx.datastore.preferences.core
                    .intPreferencesKey(key.name),
            ] = requireNotNull(source.toIntOrNull())
        }

        is String -> {
            result[stringPreferencesKey(key.name)] = source
        }

        is Set<*> -> {
            result[stringSetPreferencesKey(key.name)] =
                if (source.isEmpty()) {
                    emptySet()
                } else {
                    source.split(',').map { Base64.getDecoder().decode(it).toString(Charsets.UTF_8) }.toSet()
                }
        }

        else -> {
            throw IllegalArgumentException("Неподдерживаемый тип настройки")
        }
    }
}
