package com.solthariel.pawchan.infrastructure.thread

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.thread.ThreadRepository

/** Где искать пост по прямой ссылке на файл: тред загружен, пост найден или нет. */
internal sealed interface FilePostLookup {
    /** Явный номер, включая ОП: сохранённая позиция не заменяет переход к посту. */
    data class Found(
        val post: Long,
    ) : FilePostLookup

    data object Missing : FilePostLookup

    data object Unavailable : FilePostLookup
}

/** Пост ищется по ключу файла, предоставленному адаптером сайта. */
internal suspend fun ThreadRepository.findFilePost(
    address: ThreadAddress,
    url: String,
    filePath: (String) -> String?,
): FilePostLookup {
    val path = filePath(url)
    val snapshot = if (path == null) null else (loadThread(address) as? DomainOutcome.Success)?.value
    val post = snapshot?.posts?.firstOrNull { post -> post.attachments.any { filePath(it.remoteUrl.value) == path } }
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
            FilePostLookup.Found(post.address.post.value)
        }
    }
}
