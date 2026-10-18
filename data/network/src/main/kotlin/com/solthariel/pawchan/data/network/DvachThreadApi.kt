package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.thread.PostVote
import com.solthariel.pawchan.domain.thread.PostVoteRepository
import com.solthariel.pawchan.domain.thread.ReportingThreadRepository
import kotlinx.serialization.json.Json
import okhttp3.Call

class DvachThreadApi private constructor(
    private val callFactory: Call.Factory,
    private val decoder: ThreadPayloadDecoder,
) : ReportingThreadRepository,
    PostVoteRepository {
    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
        val request = ThreadRequestFactory.create(address)
        val live = executeDomainRequest(callFactory, request) { payload -> decoder.decode(address, payload) }
        return if (live == DomainOutcome.Failure(DomainFailure.NotFound)) loadArchived(address) else live
    }

    override suspend fun reportPost(
        address: ThreadAddress,
        post: PostId,
        comment: String,
    ): PostingOutcome<Unit> =
        executePostingRequest(
            callFactory = callFactory,
            request = reportPostRequest(address, post, comment),
            decode = ::decodeReportResponse,
        )

    override suspend fun vote(
        post: PostAddress,
        vote: PostVote,
    ): PostingOutcome<Unit> =
        executePostingRequest(
            callFactory = callFactory,
            request = postVoteRequest(post, vote),
            decode = ::decodePostVoteResponse,
        )

    private suspend fun loadArchived(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
        callFactory.executeHttpCall(
            request = boardDetailsRequest(address.board, listOf("arch", "res", "${address.thread.value}.json")),
            onTransportFailure = { it.toDomainOutcome() },
            handleResponse = { response ->
                response.toDomainOutcome { payload ->
                    decoder.decode(address, payload)?.archived(response.request.url.encodedPath)
                }
            },
        )

    companion object {
        fun create(callFactory: Call.Factory): DvachThreadApi {
            val json =
                dvachReadJson()
            return DvachThreadApi(
                callFactory = callFactory,
                decoder = ThreadPayloadDecoder(json),
            )
        }
    }
}
