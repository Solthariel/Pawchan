package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome

/** Logical identity, not the current mirror hostname. Unknown sites never borrow Makaba endpoints. */
internal fun BoardAddress.isDvachSource(): Boolean = site.value == "dvach"

internal fun unsupportedDvachSource(): DomainOutcome.Failure = DomainOutcome.Failure(DomainFailure.UnsupportedSite)

internal fun unsupportedDvachPosting(): PostingOutcome.Failure =
    PostingOutcome.Failure(PostingFailure.Rejected("Этот сайт не поддерживается адаптером 2ch"))
