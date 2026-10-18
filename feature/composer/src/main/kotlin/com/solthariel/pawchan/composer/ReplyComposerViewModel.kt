package com.solthariel.pawchan.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingRepository

/** The route's ViewModelStore owns this session; recomposition does not recreate it. */
class ReplyComposerViewModel internal constructor(
    address: ThreadAddress,
    posting: PostingRepository,
    onPosted: (BoardPost) -> Unit,
    drafts: ComposerDraftCoordinator? = null,
    prepareFiles: AttachmentPreparer = ::prepareComposerAttachments,
) : ViewModel() {
    val controller =
        ReplyComposerController(viewModelScope, address, posting, onPosted, prepareFiles, drafts)

    companion object {
        fun factory(
            address: ThreadAddress,
            posting: PostingRepository,
            onPosted: (BoardPost) -> Unit,
            drafts: ComposerDraftCoordinator? = null,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ReplyComposerViewModel(address, posting, onPosted, drafts) }
            }
    }
}
