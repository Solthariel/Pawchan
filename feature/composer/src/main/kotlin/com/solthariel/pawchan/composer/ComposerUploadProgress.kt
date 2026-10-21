package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.PostingUploadProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal fun <T> composerProgressObserver(
    job: Job?,
    state: MutableStateFlow<T>,
    transform: (T, PostingUploadProgress) -> T,
): PostingProgressObserver =
    PostingProgressObserver { progress ->
        state.update { current -> if (job?.isActive == true) transform(current, progress) else current }
    }

internal fun uploadStatusText(upload: PostingUploadProgress?): String =
    when {
        upload?.awaitingResponse == true -> "Ожидание ответа…"
        upload != null && upload.totalBytes > 0 -> "Отправка ${upload.percent}%"
        else -> "Отправка…"
    }
