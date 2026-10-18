package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.solthariel.pawchan.domain.settings.ThemePreference
import kotlinx.coroutines.flow.first

interface PortableSettings {
    fun defaults(): Map<String, String>

    suspend fun export(): Map<String, String>

    suspend fun exportOwn(): Map<String, String>

    fun validate(
        values: Map<String, String>,
        own: Map<String, String>,
    )

    suspend fun replace(
        values: Map<String, String>,
        own: Map<String, String>?,
    )
}

internal class DataStorePortableSettings(
    private val dataStore: DataStore<Preferences>,
    private val resetOverrides: () -> Unit,
) : PortableSettings {
    override fun defaults(): Map<String, String> = exportPortablePreferences(portableSchema())

    override suspend fun export(): Map<String, String> {
        val stored = dataStore.data.first()
        val effective = stored.toMutablePreferences().apply { write(stored.toAppSettings()) }
        return exportPortablePreferences(effective)
    }

    override suspend fun exportOwn(): Map<String, String> = dataStore.data.first().exportOwnMarkers()

    override fun validate(
        values: Map<String, String>,
        own: Map<String, String>,
    ) {
        val parsed = validatePortablePreferences(values)
        val target = portableSchema().toMutablePreferences()
        copyPortableTo(target, parsed)
        val normalized = exportPortablePreferences(target)
        require(values.all { (key, value) -> normalized[key] == value }) { "Недопустимое значение настройки" }
        validateOwnMarkers(own)
        values["home_board"]?.let { require(it.isEmpty() || parseHomeBoard(it) != null) }
        values["favorite_boards_v2"]?.let { boards ->
            require(
                boards.lineSequence().filter { it.isNotBlank() }.all {
                    com.solthariel.pawchan.domain.model.BoardId
                        .parse(it) != null
                },
            )
        }
        values["theme"]?.let { theme -> require(ThemePreference.entries.any { it.name == theme }) }
    }

    override suspend fun replace(
        values: Map<String, String>,
        own: Map<String, String>?,
    ) {
        validate(values, own.orEmpty())
        val parsed = validatePortablePreferences(values)
        dataStore.edit { target ->
            writePortableValues(target, parsed)
            if (own != null) target.replaceOwnMarkers(own)
        }
        resetOverrides()
    }
}
