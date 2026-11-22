package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Keeps UTF-8 conversion and HTML/JSON parsing off the caller's thread. */
internal suspend fun <T> decodeArchiveInBackground(
    dispatcher: CoroutineDispatcher,
    payload: suspend () -> DomainOutcome<String>,
    parse: (String) -> T?,
): DomainOutcome<T> =
    withContext(dispatcher) {
        val result = payload()
        currentCoroutineContext().ensureActive()
        val decoded =
            when (result) {
                is DomainOutcome.Failure -> {
                    result
                }

                is DomainOutcome.Success -> {
                    parse(result.value)?.let { DomainOutcome.Success(it) }
                        ?: DomainOutcome.Failure(DomainFailure.InvalidResponse)
                }
            }
        // Jsoup is synchronous: cancellation during parsing must still discard its result.
        currentCoroutineContext().ensureActive()
        decoded
    }
