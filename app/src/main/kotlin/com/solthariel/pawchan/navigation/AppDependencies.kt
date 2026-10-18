package com.solthariel.pawchan.navigation

import com.solthariel.pawchan.boards.BoardRepositories
import com.solthariel.pawchan.composer.ComposerDraftCoordinator
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.PostingAccessRepository
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.thread.PostVoteSession

internal data class CatalogNavigation(
    val onThreadClick: (ThreadAddress) -> Unit,
    val onLibraryClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onNewThreadClick: (BoardAddress) -> Unit,
    val onBoardSelected: (BoardAddress) -> Unit,
    val onChallenge: (String) -> Unit,
    val onArchive: (BoardAddress) -> Unit,
)

internal data class AppDependencies(
    val repositories: AppRepositories,
    val mediaLoaders: MediaLoaders,
    val drafts: ComposerDraftCoordinator,
    val postPreferences: PostPreferencesRepository? = null,
    val postVotes: PostVoteSession? = null,
    val appSettings: AppSettingsRepository? = null,
    val filterStatistics: ContentFilterStatisticsRepository? = null,
) {
    val boardDirectoryRepository: BoardDirectoryRepository
        get() = repositories.boardDirectory
    val boardPreferencesRepository: BoardPreferencesRepository
        get() = repositories.boardPreferences
    val postingAccessRepository: PostingAccessRepository
        get() = repositories.postingAccess
    val catalogRepository: CatalogRepository
        get() = repositories.catalog
    val threadRepository: ThreadRepository
        get() = repositories.thread
    val postingRepository: PostingRepository
        get() = repositories.posting
    val library: ThreadLibraryRepository
        get() = repositories.libraryThreads
    val watches: ThreadWatchRepository
        get() = repositories.watches
}

internal data class AppRepositories(
    val boards: BoardRepositories,
    val content: ContentRepositories,
    val library: LibraryRepositories,
) {
    val boardDirectory: BoardDirectoryRepository
        get() = boards.directory
    val boardPreferences: BoardPreferencesRepository
        get() = boards.preferences
    val postingAccess: PostingAccessRepository
        get() = boards.postingAccess
    val catalog: CatalogRepository
        get() = content.catalog
    val thread: ThreadRepository
        get() = content.thread
    val posting: PostingRepository
        get() = content.posting
    val libraryThreads: ThreadLibraryRepository
        get() = library.threads
    val watches: ThreadWatchRepository
        get() = library.watches
}

internal data class ContentRepositories(
    val catalog: CatalogRepository,
    val thread: ThreadRepository,
    val posting: PostingRepository,
)

internal data class LibraryRepositories(
    val threads: ThreadLibraryRepository,
    val watches: ThreadWatchRepository,
)
