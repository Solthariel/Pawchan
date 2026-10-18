package com.solthariel.pawchan.infrastructure.thread

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.thread.ThreadRepository
import java.net.URI
import java.net.URISyntaxException

/** Где искать пост по прямой ссылке на файл: тред загружен, пост найден или нет. */
internal sealed interface FilePostLookup {
    /** [post] == null — файл у ОП-поста, тред открывается сверху. */
    data class Found(
        val post: Long?,
    ) : FilePostLookup

    data object Missing : FilePostLookup

    data object Unavailable : FilePostLookup
}

/** Пост ищется по пути файла: зеркала 2ch отдают один файл с разных хостов. */
internal suspend fun ThreadRepository.findFilePost(
    address: ThreadAddress,
    url: String,
): FilePostLookup {
    val path = url.filePath()
    val snapshot = if (path == null) null else (loadThread(address) as? DomainOutcome.Success)?.value
    val post = snapshot?.posts?.firstOrNull { post -> post.attachments.any { it.remoteUrl.value.filePath() == path } }
    return when {
        path == null -> {
            FilePostLookup.Missing
        }

        snapshot == null -> {
            FilePostLookup.Unavailable
        }

        post == null -> {
            FilePostLookup.Missing
        }

        else -> {
            FilePostLookup.Found(
                post.address.post.value
                    .takeIf { it != address.thread.value },
            )
        }
    }
}

private fun String.filePath(): String? =
    try {
        URI(this).path?.takeIf(String::isNotEmpty)
    } catch (_: URISyntaxException) {
        null
    }
