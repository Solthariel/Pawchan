package com.solthariel.pawchan.settings

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/** Local first. A rejected or older response never replaces the last good history. */
internal class ChangelogHistoryRepository(
    private val cache: ChangelogCache?,
    private val bundled: () -> List<ChangelogRelease>,
    private val download: suspend (String?, Long?) -> ChangelogDownload,
) {
    private val mutex = Mutex()

    suspend fun local(): List<ChangelogRelease> =
        withContext(Dispatchers.IO) {
            val saved = historyOrNull { cache?.load()?.body?.let(::decodeRemoteChangelog) }
            mergeHistory(saved.orEmpty(), historyOrNull { bundled() }.orEmpty()).also {
                require(it.isNotEmpty()) { "History is unavailable" }
            }
        }

    suspend fun refresh(minimumCode: Long = 0): List<ChangelogRelease>? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                historyOrNull refreshed@{
                    val saved = historyOrNull { cache?.load() }
                    val current = mergeHistory(saved?.body?.let(::decodeRemoteChangelog).orEmpty(), bundled())
                    val missingVersion = minimumCode.takeIf { it > current.first().code }
                    val response = download(saved?.etag?.takeIf { missingVersion == null }, missingVersion)
                    if (response.body == null) {
                        return@refreshed current.takeIf { saved != null && missingVersion == null }
                    }
                    val remote = decodeRemoteChangelog(response.body)
                    val requiredCode = maxOf(current.first().code.toLong(), minimumCode)
                    require(remote.first().code >= requiredCode) { "Stale history" }
                    require(remote.map { it.code }.containsAll(current.map { it.code })) { "Incomplete history" }
                    val combined = mergeHistory(remote, current)
                    cache?.save(response)
                    combined
                }
            }
        }
}

internal fun mergeHistory(
    primary: List<ChangelogRelease>,
    fallback: List<ChangelogRelease>,
): List<ChangelogRelease> = (primary + fallback).distinctBy { it.code }.sortedByDescending { it.code }

private suspend fun <T> historyOrNull(block: suspend () -> T?): T? =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IllegalStateException) {
        null
    } catch (_: SecurityException) {
        null
    }
