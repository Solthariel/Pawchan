package com.solthariel.pawchan.backup

import com.solthariel.pawchan.data.drafts.WorkspaceTransfer
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal fun BackupSection.fileName(): String = name.lowercase(java.util.Locale.ROOT) + ".json"

internal fun backupPayload(backup: ClientBackup): Map<String, String> =
    backup.sections.associate { section ->
        val source =
            when (section) {
                BackupSection.SETTINGS -> {
                    stringMapJson(backup.settings.filterKeys { preferenceSection(it) == section }).toString()
                }

                BackupSection.FILTERS -> {
                    stringMapJson(backup.settings.filterKeys { preferenceSection(it) == section }).toString()
                }

                BackupSection.FAVORITES -> {
                    buildJsonObject {
                        put("boards", backup.settings["favorite_boards_v2"].orEmpty())
                        val threads = encodeThreads(backup.favorites.filter { it.favorite }, false)
                        put("threads", Json.parseToJsonElement(threads))
                    }.toString()
                }

                BackupSection.HISTORY -> {
                    encodeThreads(backup.favorites.filter { it.lastVisitedAtEpochMillis > 0L }, true)
                }

                BackupSection.HIDDEN -> {
                    WorkspaceTransfer.encode(WorkspaceTransfer.select(backup.bookmarks, true, false))
                }

                BackupSection.OWN_POSTS -> {
                    val ownState = WorkspaceTransfer.select(backup.bookmarks, false, true)
                    buildJsonObject {
                        put(
                            "workspace",
                            Json.parseToJsonElement(WorkspaceTransfer.encode(ownState)),
                        )
                        put("markers", stringMapJson(backup.ownMarkers))
                    }.toString()
                }
            }
        section.fileName() to source
    }

internal fun readBackupPayload(
    payload: Map<String, String>,
    metadata: BackupMetadata,
): ClientBackup {
    val sections = BackupSection.entries.filter { it.fileName() in payload }.toSet()
    val preferences =
        listOf(BackupSection.SETTINGS, BackupSection.FILTERS)
            .flatMap { section ->
                payload[section.fileName()]
                    ?.let { source ->
                        Json
                            .parseToJsonElement(source)
                            .jsonObject
                            .stringMap()
                            .also { map ->
                                require(map.keys.all { preferenceSection(it) == section })
                            }.toList()
                    }.orEmpty()
            }.toMap()
            .toMutableMap()
    val favoriteSource = payload[BackupSection.FAVORITES.fileName()]?.let { Json.parseToJsonElement(it).jsonObject }
    if (favoriteSource != null) {
        preferences["favorite_boards_v2"] = favoriteSource.getValue("boards").jsonPrimitive.content
    }
    val favorites = favoriteSource?.let { decodeThreads(it.getValue("threads").toString(), false) }.orEmpty()
    val history = payload[BackupSection.HISTORY.fileName()]?.let { decodeThreads(it, true) }.orEmpty()
    val visits = history.associateBy { it.address }
    val favoriteAddresses = favorites.map { it.address }.toSet()
    val entries =
        favorites.map { it.copy(lastVisitedAtEpochMillis = visits[it.address]?.lastVisitedAtEpochMillis ?: 0L) } +
            history.filterNot { it.address in favoriteAddresses }
    val hidden = payload[BackupSection.HIDDEN.fileName()]?.let(WorkspaceTransfer::decode) ?: WorkspaceState()
    val own = payload[BackupSection.OWN_POSTS.fileName()]?.let { Json.parseToJsonElement(it).jsonObject }
    val ownState = own?.getValue("workspace")?.toString()?.let(WorkspaceTransfer::decode) ?: WorkspaceState()
    return ClientBackup(
        preferences,
        entries,
        ownState.copy(marks = hidden.marks),
        sections,
        own
            ?.getValue("markers")
            ?.jsonObject
            ?.stringMap()
            .orEmpty(),
        metadata,
    ).also { validateBackupRecords(it) }
}

internal fun preferenceSection(key: String): BackupSection =
    when (key) {
        "app_content_filters_v1" -> BackupSection.FILTERS
        "favorite_boards_v2" -> BackupSection.FAVORITES
        else -> BackupSection.SETTINGS
    }

internal fun backupCounts(backup: ClientBackup): Map<BackupSection, Int> =
    backup.sections.associateWith { section ->
        when (section) {
            BackupSection.SETTINGS -> {
                backup.settings.keys.count { preferenceSection(it) == section }
            }

            BackupSection.FILTERS -> {
                backup.settings["app_content_filters_v1"]
                    .orEmpty()
                    .lineSequence()
                    .count { it.isNotBlank() }
            }

            BackupSection.FAVORITES -> {
                backup.favorites.count { it.favorite } +
                    backup.settings["favorite_boards_v2"]
                        .orEmpty()
                        .lineSequence()
                        .count { it.isNotBlank() }
            }

            BackupSection.HISTORY -> {
                backup.favorites.count { it.lastVisitedAtEpochMillis > 0L }
            }

            BackupSection.HIDDEN -> {
                backup.bookmarks.marks.hidden.size
            }

            BackupSection.OWN_POSTS -> {
                backup.bookmarks.posts.size + backup.bookmarks.replies.size
            }
        }
    }

private fun validateBackupRecords(backup: ClientBackup) {
    val markerCount =
        backup.ownMarkers.values.sumOf { value ->
            if (value.isEmpty()) 0 else value.count { it == ',' } + 1
        }
    require(markerCount <= BACKUP_MAX_RECORDS)
    val state = backup.bookmarks
    require(backup.favorites.size <= BACKUP_MAX_RECORDS)
    require(state.posts.size <= BACKUP_MAX_RECORDS && state.replies.size <= BACKUP_MAX_RECORDS)
    require(state.marks.hidden.size <= BACKUP_MAX_RECORDS && backup.ownMarkers.size <= BACKUP_MAX_RECORDS)
    require(state.posts.distinctBy { it.post.address }.size == state.posts.size)
    require(state.replies.distinctBy { it.post.address }.size == state.replies.size)
}
