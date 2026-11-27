package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.settings.DEFAULT_SHOW_HIDDEN_POSTS
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Prepared repository and borrowed loaders; feature owns the preview ViewModel session. */
data class PostPreviewResources(
    val repository: ThreadRepository?,
    val thumbnails: ThumbnailLoader,
    val actions: PostPreviewActions,
    val contentFilters: Flow<List<ContentFilterRule>> = flowOf(emptyList()),
    val showHiddenPosts: Flow<Boolean> = flowOf(DEFAULT_SHOW_HIDDEN_POSTS),
)

/** App handles persisted effects and navigation; no thread UI state crosses this port. */
interface PostPreviewActions {
    fun canReply(address: ThreadAddress): Boolean = true

    fun prepareReply(
        address: ThreadAddress,
        text: String,
    )

    fun navigate(address: ThreadAddress)
}
