package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.AttachmentId
import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardDescriptor
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.ByteSize
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.CatalogThread
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadStatistics
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

internal class CatalogPayloadDecoder(
    private val json: Json,
) {
    fun decode(payload: String): CatalogSnapshot? {
        val dto = json.decodeFromString<CatalogDto>(payload)
        val boardAddress = BoardId.parse(dto.board.id)?.let { BoardAddress(DVACH_SITE, it) }
        val threads = boardAddress?.let { dto.threads.toDomain(json, it) }
        return if (boardAddress != null && threads != null) {
            CatalogSnapshot(
                board =
                    BoardDescriptor(
                        address = boardAddress,
                        title = dto.board.name,
                        worksafe = false,
                        subjectsEnabled = dto.board.subjectsEnabled,
                    ),
                threads =
                    threads.map { thread ->
                        CatalogThread(
                            thread.address,
                            thread.subject,
                            thread.content.copy(
                                metadata =
                                    thread.content.metadata.copy(
                                        subject =
                                            if (dto.board.subjectsEnabled ==
                                                false
                                            ) {
                                                null
                                            } else {
                                                thread.subject
                                            },
                                    ),
                            ),
                            thread.attachments,
                            thread.statistics,
                            thread.flags,
                            thread.createdAtEpochSeconds,
                        )
                    },
                boardSpeed = dto.extractBoardSpeed(),
            )
        } else {
            null
        }
    }
}

private fun CatalogThreadDto.toDomain(
    json: Json,
    board: BoardAddress,
): CatalogThread? {
    val address = ThreadId.parse(num)?.let { ThreadAddress(board, it) }
    val document = PostDocumentParser.parse(comment)
    val parsedSubject = subject.decodeHtmlText().takeIf(String::isNotBlank)
    val attachments = files.orEmpty().toDomainAttachments(json)
    val statistics =
        ThreadStatistics.create(
            replies = postsCount,
            files = filesCount,
            images = attachments.count(MediaAttachment::isImage),
            knownMediaBytes = attachments.sumOf { attachment -> attachment.byteSize?.value ?: 0L },
            mediaBytesComplete =
                attachments.size == filesCount &&
                    attachments.all { attachment -> attachment.byteSize != null },
        )
    return if (address != null && statistics != null) {
        CatalogThread(
            address = address,
            subject = parsedSubject,
            content =
                PostContent(
                    document = document,
                    metadata = toPostMetadata(),
                ),
            attachments = attachments,
            statistics = statistics,
            flags =
                ThreadFlags(
                    pinned = sticky > 0,
                    closed = closed > 0,
                    archived = false,
                    endless = endless > 0,
                ),
            createdAtEpochSeconds = (timestamp as? JsonPrimitive)?.longOrNull?.takeIf { it > 0L },
        )
    } else {
        null
    }
}

private fun CatalogThreadDto.toPostMetadata(): PostMetadata {
    val identity = decodeDvachPostIdentity(name, email)
    return PostMetadata(
        name = identity.name,
        date = date?.takeIf(String::isNotBlank),
        sage = identity.sage,
    )
}

private fun AttachmentDto.toDomain(): MediaAttachment? {
    val fileName = displayname?.takeIf(String::isNotBlank) ?: name
    val sizeInBytes = size?.toBytesOrNull()
    val id = AttachmentId.parse(md5?.takeIf(String::isNotBlank) ?: path)
    val remoteUrl = path.toRemoteUrl()
    val thumbnailUrl = thumbnail?.toRemoteUrl()
    val hasValidSize = size == null || sizeInBytes != null
    val hasValidThumbnail = thumbnail == null || thumbnailUrl != null
    val hasValidMetadata = fileName.isNotBlank() && hasValidSize && hasValidThumbnail
    return if (hasValidMetadata && id != null && remoteUrl != null) {
        MediaAttachment(
            id = id,
            fileName = fileName,
            source = AttachmentSource(remoteUrl, thumbnailUrl),
            byteSize = sizeInBytes,
            kind = mediaKind(name),
            dimensions = MediaDimensions.create(width, height),
            isSticker = type == 100 || path.startsWith("/stickers/"),
        )
    } else {
        null
    }
}

private fun List<JsonElement>.toDomain(
    json: Json,
    board: BoardAddress,
): List<CatalogThread> =
    mapNotNull { element ->
        json.decodeElementOrNull<CatalogThreadDto>(element)?.toDomain(json, board)
    }

internal fun List<JsonElement>.toDomainAttachments(json: Json): List<MediaAttachment> =
    mapNotNull { element ->
        json.decodeElementOrNull<AttachmentDto>(element)?.toDomain()
    }

private fun Long.toBytesOrNull(): ByteSize? =
    takeIf { it in 0..MAX_KIBIBYTES }
        ?.times(BYTES_PER_KIBIBYTE)
        ?.let(ByteSize::parse)

private fun String.toRemoteUrl(): RemoteUrl? =
    takeIf { startsWith('/') && !startsWith("//") }?.let { path ->
        RemoteUrl.parse("$DVACH_ORIGIN$path")
    }

private fun mediaKind(fileName: String): MediaKind =
    when (fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "jpg", "jpeg", "png", "webp" -> MediaKind.IMAGE
        "gif" -> MediaKind.ANIMATION
        "webm", "mp4" -> MediaKind.VIDEO
        "mp3", "ogg", "opus", "wav" -> MediaKind.AUDIO
        else -> MediaKind.FILE
    }

private fun MediaAttachment.isImage(): Boolean = kind == MediaKind.IMAGE || kind == MediaKind.ANIMATION

private val DVACH_SITE = requireNotNull(SiteId.parse("dvach"))
private const val BYTES_PER_KIBIBYTE = 1024L
private const val MAX_KIBIBYTES = Long.MAX_VALUE / BYTES_PER_KIBIBYTE
