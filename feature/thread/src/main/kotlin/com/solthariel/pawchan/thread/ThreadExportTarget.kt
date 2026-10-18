package com.solthariel.pawchan.thread

/** One chosen document. The app owns opening, closing and rendering to its destination. */
interface ThreadExportTarget {
    suspend fun saveHtml(document: ThreadExportDocument)

    suspend fun saveShot(
        posts: List<ThreadExportPost>,
        style: ThreadShotStyle,
    )
}
