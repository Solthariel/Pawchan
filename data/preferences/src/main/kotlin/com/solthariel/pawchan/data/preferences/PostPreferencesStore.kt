package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.postpreferences.PostPreferences
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import kotlinx.coroutines.flow.first
import java.io.IOException

internal class PostPreferencesStore(
    private val dataStore: DataStore<Preferences>,
) : PostPreferencesRepository {
    override suspend fun loadPostPreferences(address: ThreadAddress): DomainOutcome<PostPreferences> =
        try {
            val stored = dataStore.data.first()
            DomainOutcome.Success(
                PostPreferences(
                    own = stored.ids(address, "own"),
                    hidden = stored.ids(address, "hidden"),
                    replyTrees = stored.ids(address, "trees"),
                    names = stored[key(address, "names")].orEmpty(),
                    similar = stored[key(address, "similar")].orEmpty(),
                    shown = stored.ids(address, "shown"),
                ),
            )
        } catch (_: IOException) {
            DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        }

    override suspend fun savePostPreferences(
        address: ThreadAddress,
        value: PostPreferences,
    ): DomainOutcome<Unit> =
        try {
            dataStore.edit { stored ->
                stored[key(address, "own")] = value.own.map(Long::toString).toSet()
                stored[key(address, "hidden")] = value.hidden.map(Long::toString).toSet()
                stored[key(address, "trees")] = value.replyTrees.map(Long::toString).toSet()
                stored[key(address, "names")] = value.names
                stored[key(address, "similar")] = value.similar
                stored[key(address, "shown")] = value.shown.map(Long::toString).toSet()
            }
            DomainOutcome.Success(Unit)
        } catch (_: IOException) {
            DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        }
}

private fun key(
    address: ThreadAddress,
    field: String,
): Preferences.Key<Set<String>> =
    stringSetPreferencesKey("post:${address.site.value}:${address.board.board.value}:${address.thread.value}:$field")

private fun Preferences.ids(
    address: ThreadAddress,
    field: String,
): Set<Long> = this[key(address, field)].orEmpty().mapNotNull { it.toLongOrNull()?.takeIf { id -> id > 0 } }.toSet()
