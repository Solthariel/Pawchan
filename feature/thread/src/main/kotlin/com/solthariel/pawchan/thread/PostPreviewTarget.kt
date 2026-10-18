package com.solthariel.pawchan.thread

import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId

internal data class PostPreviewTarget(
    val address: ThreadAddress?,
    val postId: Long,
    val replies: Boolean = false,
)

internal val LocalPostPreview = staticCompositionLocalOf<((PostLink.Reference) -> Unit)?> { null }

internal fun referenceToken(reference: PostLink.Reference): String =
    reference.thread?.let {
        "${reference.post.value}|${it.site.value}|${it.board.board.value}|${it.thread.value}"
    } ?: reference.post.value.toString()

internal fun parseReferenceToken(token: String): PostLink.Reference? {
    val parts = token.split('|')
    val id = parts.firstOrNull()?.toLongOrNull()?.let(PostId::parse)
    val thread = if (parts.size == REFERENCE_PARTS) parseReferenceThread(parts) else null
    return if (id != null && (parts.size == 1 || thread != null)) PostLink.Reference(id, thread) else null
}

private fun parseReferenceThread(parts: List<String>): ThreadAddress? {
    val site = SiteId.parse(parts[1])
    val board = BoardId.parse(parts[2])
    val thread = parts[REFERENCE_THREAD_PART].toLongOrNull()?.let(ThreadId::parse)
    return if (site != null && board != null &&
        thread != null
    ) {
        ThreadAddress(BoardAddress(site, board), thread)
    } else {
        null
    }
}

private const val REFERENCE_PARTS = 4
private const val REFERENCE_THREAD_PART = 3
