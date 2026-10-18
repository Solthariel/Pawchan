package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader

/** Prepared repository and borrowed loaders; feature owns the preview ViewModel session. */
data class PostPreviewResources(
    val repository: ThreadRepository?,
    val thumbnails: ThumbnailLoader,
    val actions: PostPreviewActions,
    val contentFilters: kotlinx.coroutines.flow.Flow<List<com.solthariel.pawchan.domain.filter.ContentFilterRule>> =
        kotlinx.coroutines.flow.flowOf(emptyList()),
)

/** App handles persisted effects and navigation; no thread UI state crosses this port. */
interface PostPreviewActions {
    fun prepareReply(
        address: ThreadAddress,
        text: String,
    )

    fun navigate(address: ThreadAddress)
}
