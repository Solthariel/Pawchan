package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.solthariel.pawchan.boards.BoardRepositories
import com.solthariel.pawchan.infrastructure.AppNetworkViewModel
import com.solthariel.pawchan.infrastructure.AppStorageViewModel

@Composable
internal fun rememberAppDependencies(
    network: AppNetworkViewModel,
    storage: AppStorageViewModel,
): AppDependencies =
    remember(network.stack, storage.library, storage.settings, storage.recentBoards) {
        AppDependencies(
            repositories =
                AppRepositories(
                    boards =
                        BoardRepositories(
                            directory = network.stack.boardDirectoryRepository,
                            details = network.stack.boardDetailsRepository,
                            settings = storage.settings,
                            preferences = storage.settings,
                            postingAccess = network.stack.postingAccessRepository,
                            recent = storage.recentBoards,
                        ),
                    content =
                        ContentRepositories(
                            catalog = network.stack.catalogRepository,
                            thread = network.stack.threadRepository,
                            posting =
                                com.solthariel.pawchan.infrastructure.UsagePostingRepository(
                                    com.solthariel.pawchan.infrastructure.PostCountPostingRepository(
                                        network.stack.postingRepository,
                                        storage.settings,
                                        storage.settings.usage,
                                    ),
                                    storage.settings.usage,
                                ),
                        ),
                    library =
                        LibraryRepositories(
                            threads = storage.library,
                            watches = storage.watches,
                        ),
                ),
            mediaLoaders = network.stack.mediaLoaders,
            drafts = storage.drafts,
            postPreferences = storage.postPreferences,
            appSettings = storage.settings,
            filterStatistics = storage.filterStatistics,
            postVotes = network.stack.postVotes,
        )
    }
