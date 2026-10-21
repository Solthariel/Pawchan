package com.solthariel.pawchan.domain.posting

/** Bytes written to the transport, not confirmation that the server accepted a post. */
data class PostingUploadProgress(
    val sentBytes: Long,
    val totalBytes: Long,
    val awaitingResponse: Boolean = false,
) {
    init {
        require(totalBytes >= 0 && sentBytes in 0..totalBytes)
    }

    val percent: Int get() = if (totalBytes == 0L) 0 else (sentBytes * PERCENT_SCALE / totalBytes).toInt()
}

fun interface PostingProgressObserver {
    fun onProgress(progress: PostingUploadProgress)

    companion object {
        val None: PostingProgressObserver = PostingProgressObserver { }
    }
}

private const val PERCENT_SCALE = 100L
