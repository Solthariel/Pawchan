package com.solthariel.pawchan.infrastructure

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.archive.ArchiveController
import com.solthariel.pawchan.archive.ArchiveServices
import com.solthariel.pawchan.archive.ArchiveWorkScheduler
import com.solthariel.pawchan.composer.ComposerDraftCoordinator
import com.solthariel.pawchan.data.database.RoomStorage
import com.solthariel.pawchan.data.database.SqliteFilterStatisticsRepository
import com.solthariel.pawchan.data.drafts.FileComposerDraftRepository
import com.solthariel.pawchan.data.drafts.FileThreadHistoryRepository
import com.solthariel.pawchan.data.drafts.FileWorkspaceRepository
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import com.solthariel.pawchan.data.preferences.PreferencesRecentBoardsRepository
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.workspace.WorkspaceController
import java.io.File

internal class AppStorageViewModel private constructor(
    private val storage: RoomStorage,
    val settings: DataStoreSettingsRepository,
    draftsDirectory: File,
    private val appContext: Context,
) : ViewModel() {
    val recentBoards = PreferencesRecentBoardsRepository(appContext)
    val workspace =
        WorkspaceController(
            viewModelScope,
            FileWorkspaceRepository(File(draftsDirectory.parentFile, "workspace")),
            FileThreadHistoryRepository(File(draftsDirectory.parentFile, "threads")),
            statistics = settings.usage,
        )
    private val archiveDirectory = requireNotNull(draftsDirectory.parentFile)
    private var archiveController: ArchiveController? = null

    fun archives(loaders: MediaLoaders): ArchiveController =
        archiveController ?: ArchiveController(
            viewModelScope,
            workspace,
            ArchiveServices(archiveDirectory, workspace.repository, loaders),
            {
                ArchiveWorkScheduler
                    .enqueue(appContext)
            },
        ).also { archiveController = it }

    private val filterStore = SqliteFilterStatisticsRepository(appContext)
    val filterStatistics = UsageFilterStatistics(filterStore, settings.usage)
    val postPreferences: PostPreferencesRepository = settings
    val drafts = ComposerDraftCoordinator.create(viewModelScope, FileComposerDraftRepository(draftsDirectory))
    val library: ThreadLibraryRepository = UsageThreadLibrary(storage.library, settings.usage)
    val watches: ThreadWatchRepository = storage.watches

    override fun onCleared() {
        filterStore.close()
        storage.close()
    }

    companion object {
        fun factory(
            context: Context,
            settings: DataStoreSettingsRepository,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AppStorageViewModel(
                        storage = RoomStorage.create(context),
                        settings = settings,
                        draftsDirectory = File(context.applicationContext.filesDir, "composer-drafts"),
                        appContext = context.applicationContext,
                    )
                }
            }
    }
}
