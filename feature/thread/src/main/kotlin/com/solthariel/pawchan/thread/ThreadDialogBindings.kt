package com.solthariel.pawchan.thread

import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader

internal data class ThreadDialogBindings(
    val appearance: ThreadAppearance,
    val onAppearance: (ThreadAppearance) -> Unit,
    val reports: ThreadReportController?,
    val thumbnails: ThumbnailLoader,
    val archive: ThreadArchiveHost,
    val history: ThreadHistoryActions? = null,
    val hiddenRules: ThreadPostPreferencesController? = null,
)
