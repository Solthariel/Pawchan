package com.solthariel.pawchan.thread

import com.solthariel.pawchan.core.ui.RefreshFailure
import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.filter.ContentFilterSample
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaGalleryContent

internal data class ThreadContentPresentation(
    val isFavorite: Boolean,
    val isRefreshing: Boolean = false,
    val refreshFailure: RefreshFailure? = null,
    val gallery: MediaGalleryContent = MediaGalleryContent(),
    val navigation: ThreadPostNavigation = ThreadPostNavigation(),
    val contentFilters: ThreadContentFilterState = ThreadContentFilterState(),
) {
    val postPositions: Map<Long, Int>
        get() = navigation.postPositions
    val replyIndex: Map<Long, ThreadReplyGroup>
        get() = navigation.replyIndex
    val mediaGroups: Map<Long, List<MediaAttachmentUi>>
        get() = navigation.mediaGroups
    val contentFilterNotice: String?
        get() = contentFilters.notice
    val filteredComments: Int
        get() = contentFilters.filteredComments
    val filterSamples: List<ContentFilterSample>
        get() = contentFilters.samples
}

/** Prepared indices keep post/reply/media navigation together without dropping hidden post addresses. */
internal data class ThreadPostNavigation(
    val postPositions: Map<Long, Int> = emptyMap(),
    val replyIndex: Map<Long, ThreadReplyGroup> = emptyMap(),
    val mediaGroups: Map<Long, List<MediaAttachmentUi>> = emptyMap(),
)

/** Explanations and loaded-data samples belong to this filtered snapshot, not persistent settings. */
internal data class ThreadContentFilterState(
    val notice: String? = null,
    val filteredComments: Int = 0,
    val samples: List<ContentFilterSample> = emptyList(),
    val hits: List<ContentFilterHit> = emptyList(),
)
