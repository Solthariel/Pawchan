package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response

/** Only dated archive redirects on the responding origin may be followed. */
internal class DvachArchivedThreadLoader(
    private val calls: Call.Factory,
    private val decoder: ThreadPayloadDecoder,
) {
    suspend fun load(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
        var request = boardDetailsRequest(address.board, listOf("arch", "res", "${address.thread.value}.json"))
        repeat(MAX_ARCHIVE_REDIRECTS + 1) { attempt ->
            val step =
                calls.executeHttpCall(
                    request = request,
                    onTransportFailure = { ArchiveStep.Complete(it.toDomainOutcome()) },
                    handleResponse = { response ->
                        val redirected = response.archiveRedirect(address)
                        if (redirected != null && attempt < MAX_ARCHIVE_REDIRECTS) {
                            ArchiveStep.Redirect(redirected)
                        } else {
                            ArchiveStep.Complete(
                                response.toDomainOutcome { payload ->
                                    decoder.decode(address, payload)?.archived(response.request.url.encodedPath)
                                },
                            )
                        }
                    },
                )
            when (step) {
                is ArchiveStep.Complete -> return step.outcome
                is ArchiveStep.Redirect -> request = step.request
            }
        }
        return DomainOutcome.Failure(DomainFailure.InvalidResponse)
    }
}

private fun Response.archiveRedirect(address: ThreadAddress): Request? {
    val origin = request.url
    val target = if (isRedirect) header("Location")?.let(origin::resolve) else null
    if (target == null) return null
    val path = "/${address.board.board.value}/arch/[0-9]{4}-[0-9]{2}-[0-9]{2}/res/${address.thread.value}\\.json"
    val allowed =
        target.scheme == "https" && target.host == origin.host && target.port == origin.port &&
            target.username.isEmpty() && target.password.isEmpty() &&
            target.query == null && target.fragment == null && Regex(path).matches(target.encodedPath)
    return if (allowed) request.newBuilder().url(target).build() else null
}

private sealed interface ArchiveStep {
    data class Redirect(
        val request: Request,
    ) : ArchiveStep

    data class Complete(
        val outcome: DomainOutcome<ThreadSnapshot>,
    ) : ArchiveStep
}

private const val MAX_ARCHIVE_REDIRECTS = 3
