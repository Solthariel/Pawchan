package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository

internal class SmokeThreadRepository : ThreadRepository {
    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
        val post =
            BoardPost(
                PostAddress(address, requireNotNull(PostId.parse(43L))),
                PostContent(PostDocument.plain("Ответ"), PostMetadata("Аноним", null, false)),
                emptyList(),
            )
        return DomainOutcome.Success(ThreadSnapshot(address, "Тестовый тред", ThreadFlags(false, false, false), listOf(post)))
    }
}
