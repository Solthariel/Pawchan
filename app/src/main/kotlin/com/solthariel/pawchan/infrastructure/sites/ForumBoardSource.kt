package com.solthariel.pawchan.infrastructure.sites

import androidx.annotation.DrawableRes
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.SiteId

/** Registered, implemented sources only. Unknown capabilities fail closed. */
internal data class ForumCapabilities(
    val replies: Boolean = false,
    val newThreads: Boolean = false,
    val boardArchive: Boolean = false,
    val boardInformation: Boolean = false,
    val boardSpeed: Boolean = false,
    val tags: Boolean = false,
    val archiveBrowser: Boolean = false,
)

internal data class ForumBoardSource(
    val site: SiteId,
    val title: String,
    val directory: BoardDirectoryRepository,
    val capabilities: ForumCapabilities = ForumCapabilities(),
    @param:DrawableRes val icon: Int? = null,
)
