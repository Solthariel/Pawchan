package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.AttachmentId
import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ByteSize
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.RemoteUrl

internal fun FourChanPostDto.attachment(board: BoardAddress): MediaAttachment? {
    val stamp = tim?.takeIf { it > 0 }
    val extension = ext?.lowercase()?.takeIf { it in FOUR_CHAN_EXTENSIONS }
    val code = board.board.value.takeIf { it.matches(FOUR_CHAN_BOARD) }
    // The current domain/UI cannot safely represent a 4chan spoiler thumbnail yet.
    if (filedeleted != 0 || spoiler != 0) return null
    return if (stamp != null && extension != null && code != null) {
        val original = filename?.trim()?.takeIf { it.isNotEmpty() } ?: stamp.toString()
        val id = requireNotNull(AttachmentId.parse(stamp.toString()))
        val url = requireNotNull(RemoteUrl.parse("https://i.4cdn.org/$code/$stamp$extension"))
        val thumb = RemoteUrl.parse("https://i.4cdn.org/$code/${stamp}s.jpg")
        val kind =
            when (extension) {
                ".gif" -> MediaKind.ANIMATION
                ".webm", ".mp4" -> MediaKind.VIDEO
                else -> MediaKind.IMAGE
            }
        MediaAttachment(
            id,
            "$original$extension",
            AttachmentSource(url, thumb),
            fsize?.let(ByteSize::parse),
            kind,
            MediaDimensions.create(w, h),
        )
    } else {
        null
    }
}

internal val FOUR_CHAN_BOARD = Regex("[a-z0-9]+")
private val FOUR_CHAN_EXTENSIONS = setOf(".jpg", ".jpeg", ".png", ".gif", ".webm", ".mp4")
