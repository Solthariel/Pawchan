package com.solthariel.pawchan.domain.model

@JvmInline
value class SiteId private constructor(
    val value: String,
) {
    companion object {
        fun parse(raw: String): SiteId? = normalizeSegment(raw)?.let(::SiteId)
    }
}

@JvmInline
value class BoardId private constructor(
    val value: String,
) {
    companion object {
        fun parse(raw: String): BoardId? = normalizeSegment(raw)?.let(::BoardId)
    }
}

@JvmInline
value class ThreadId private constructor(
    val value: Long,
) {
    companion object {
        fun parse(raw: Long): ThreadId? = raw.takeIf { it > 0L }?.let(::ThreadId)
    }
}

@JvmInline
value class PostId private constructor(
    val value: Long,
) {
    companion object {
        fun parse(raw: Long): PostId? = raw.takeIf { it > 0L }?.let(::PostId)
    }
}

@JvmInline
value class AttachmentId private constructor(
    val value: String,
) {
    companion object {
        fun parse(raw: String): AttachmentId? = raw.trim().takeIf(String::isNotEmpty)?.let(::AttachmentId)
    }
}

@JvmInline
value class ByteSize private constructor(
    val value: Long,
) {
    companion object {
        fun parse(raw: Long): ByteSize? = raw.takeIf { it >= 0L }?.let(::ByteSize)
    }
}

@JvmInline
value class RemoteUrl private constructor(
    val value: String,
) {
    companion object {
        fun parse(raw: String): RemoteUrl? {
            val normalized = raw.trim()
            return normalized.takeIf { it.startsWith(HTTPS_PREFIX) }?.let(::RemoteUrl)
        }
    }
}

private fun normalizeSegment(raw: String): String? {
    val normalized = raw.trim().lowercase()
    return normalized.takeIf { value ->
        value.isNotEmpty() &&
            value.all { character ->
                character.isLetterOrDigit() || character == '_' || character == '-'
            }
    }
}

private const val HTTPS_PREFIX = "https://"
