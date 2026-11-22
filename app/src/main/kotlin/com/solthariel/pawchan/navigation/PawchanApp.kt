package com.solthariel.pawchan.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.archive.ArchiveController
import com.solthariel.pawchan.archive.ArchiveUpdatePreference
import com.solthariel.pawchan.archive.BindArchiveNotices
import com.solthariel.pawchan.composer.ComposerDraftNotices
import com.solthariel.pawchan.composer.LocalComposerPreferences
import com.solthariel.pawchan.core.ui.AppNoticeHost
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.infrastructure.AppNetworkStack
import com.solthariel.pawchan.infrastructure.AppNetworkViewModel
import com.solthariel.pawchan.infrastructure.AppPreferencesViewModel
import com.solthariel.pawchan.infrastructure.AppStorageViewModel
import com.solthariel.pawchan.infrastructure.LocalProxyCatalog
import com.solthariel.pawchan.infrastructure.LocalProxyRuntime
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.infrastructure.media.MediaPresentationHost
import com.solthariel.pawchan.media.LocalMediaPreferences
import com.solthariel.pawchan.media.thumbnail.LocalThumbnailLoader
import com.solthariel.pawchan.media.video.setVideoStartMuted
import com.solthariel.pawchan.settings.SettingsViewModel
import com.solthariel.pawchan.settings.toPostDisplay
import com.solthariel.pawchan.ui.PawchanTheme
import com.solthariel.pawchan.watch.ThreadWatchScheduler
import com.solthariel.pawchan.workspace.BindWorkspaceLifecycle
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.WorkspaceBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@Composable
internal fun PawchanApp() {
    val context = LocalContext.current
    val preferenceOwner: AppPreferencesViewModel =
        viewModel(
            factory =
                AppPreferencesViewModel
                    .factory(context),
        )
    val settings: SettingsViewModel =
        viewModel(
            factory = SettingsViewModel.factory(preferenceOwner.settings, preferenceOwner.settings),
        )
    val loaded by settings.loaded.collectAsStateWithLifecycle()
    val proxyLoad by produceState<ProxyStartup>(ProxyStartup.Loading, preferenceOwner.settings) {
        value =
            try {
                ProxyStartup.Ready(
                    preferenceOwner.settings.proxyPreferences.preferences
                        .first(),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ProxyStartup.Failed
            }
    }
    when (val state = proxyLoad) {
        ProxyStartup.Loading -> {
            Unit
        }

        ProxyStartup.Failed -> {
            Text("Не удалось прочитать настройки прокси. Подключения остановлены.")
        }

        is ProxyStartup.Ready -> {
            loaded?.let { initial -> LoadedPawchanApp(settings, preferenceOwner.settings, initial, state.preferences) }
        }
    }
}

private sealed interface ProxyStartup {
    data object Loading : ProxyStartup

    data object Failed : ProxyStartup

    data class Ready(
        val preferences: com.solthariel.pawchan.domain.proxy.ProxyPreferences,
    ) : ProxyStartup
}

@Composable
private fun LoadedPawchanApp(
    settings: SettingsViewModel,
    repository: DataStoreSettingsRepository,
    initial: AppSettings,
    initialProxies: com.solthariel.pawchan.domain.proxy.ProxyPreferences,
) {
    val context = LocalContext.current
    val network: AppNetworkViewModel =
        viewModel(
            factory =
                AppNetworkViewModel.factory(
                    context,
                    repository,
                    initialProxies,
                    initial.behavior.client.storage.cacheMb,
                    initial.behavior.client.mirror,
                ),
        )
    val storage: AppStorageViewModel =
        viewModel(
            factory = AppStorageViewModel.factory(context, repository),
        )
    val dependencies = rememberAppDependencies(network, storage)
    val archives = remember(storage, dependencies) { storage.archives(dependencies.mediaLoaders) }
    val loadedSettings by settings.loaded.collectAsStateWithLifecycle()
    val appSettings = loadedSettings ?: AppSettings()
    BindAppSettingsEffects(appSettings, archives, dependencies.mediaLoaders, network.stack)
    androidx.compose.runtime.SideEffect { storage.workspace.readingPreferences = appSettings.behavior.client.reading }
    BindWorkspaceLifecycle(storage.workspace)
    BindArchiveNotices(archives)

    CompositionLocalProvider(
        LocalProxyRuntime provides network.proxies,
        LocalProxyCatalog provides network.proxyCatalog,
    ) {
        com.solthariel.pawchan.infrastructure.MirrorSessionHost(network.stack) {
            AppSessionSurface(appSettings, storage, dependencies, archives, settings, network.stack.github)
        }
    }
}

@Composable
private fun AppSessionSurface(
    appSettings: AppSettings,
    storage: AppStorageViewModel,
    dependencies: AppDependencies,
    archives: ArchiveController,
    settings: SettingsViewModel,
    github: com.solthariel.pawchan.infrastructure.GithubNetwork,
) {
    val theme by settings.theme.collectAsStateWithLifecycle()
    val home by settings.homeBoard.collectAsStateWithLifecycle()
    val workspaceReady by storage.workspace.ready.collectAsStateWithLifecycle()
    val colors by settings.themeColors.collectAsStateWithLifecycle()
    com.solthariel.pawchan.core.ui.ModalLayerHost {
        PawchanTheme(preference = theme, colors = colors, typography = appSettings.behavior.client.reading.typography) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                AppNoticeHost {
                    if (workspaceReady) {
                        home?.let { loaded ->
                            CompositionLocalProvider(
                                com.solthariel.pawchan.infrastructure.LocalGithubNetwork provides github,
                                LocalPostDisplayPreferences provides rememberPhonePostDisplay(appSettings),
                                com.solthariel.pawchan.core.ui.LocalTopBarTransparency provides
                                    appSettings.behavior.client.reading.navigation.topBarTransparencyPercent,
                                com.solthariel.pawchan.navigation.chrome.LocalBottomBarTransparency provides
                                    appSettings.behavior.client.reading.navigation.bottomBarTransparencyPercent,
                                LocalComposerPreferences provides appSettings.behavior.client.composer,
                                LocalMediaPreferences provides appSettings.behavior.client.media,
                                com.solthariel.pawchan.media.LocalMediaViewEvents provides
                                    remember(storage.workspace) { mediaViewEvents(storage.workspace) },
                                LocalWorkspace provides
                                    WorkspaceBinding(
                                        storage.workspace,
                                        dependencies.mediaLoaders,
                                        {},
                                        storage.library,
                                        archives,
                                        storage.settings,
                                        dependencies.repositories.boards.sources,
                                    ),
                                com.solthariel.pawchan.media.LocalHiddenStickerEvents provides
                                    remember(storage.settings.usage) {
                                        com.solthariel.pawchan.media.HiddenStickerEvents { identity ->
                                            storage.settings.usage.record(
                                                com.solthariel.pawchan.domain.statistics.UsageMetric.STICKERS_HIDDEN,
                                                identity = identity,
                                            )
                                        }
                                    },
                                LocalThumbnailLoader provides dependencies.mediaLoaders.thumbnailLoader,
                            ) {
                                MediaPresentationHost {
                                    PawchanNavHost(dependencies, settings, loaded.board ?: defaultBoardAddress())
                                }
                            }
                        }
                    }
                    ComposerDraftNotices(storage.drafts)
                }
            }
        }
    }
}

/** Настройки долгоживущих сервисов: видео и обновление копий. */
@Composable
private fun BindAppSettingsEffects(
    appSettings: AppSettings,
    archives: ArchiveController,
    loaders: MediaLoaders,
    stack: AppNetworkStack,
) {
    LaunchedEffect(stack, appSettings.behavior.client.connection) {
        stack.mirror.recovery?.configure(appSettings.behavior.client.connection)
    }
    val context = LocalContext.current
    androidx.compose.runtime.SideEffect {
        stack.clearCacheOnExit = appSettings.behavior.client.media.clearCacheOnExit
        loaders.mediaDownloadQueue.preferences = appSettings.behavior.client.downloads
    }
    LaunchedEffect(appSettings.behavior.videoStartMuted) { setVideoStartMuted(appSettings.behavior.videoStartMuted) }
    LaunchedEffect(archives, appSettings.behavior.updateSavedCopies) {
        archives.updatesEnabled = appSettings.behavior.updateSavedCopies
        ArchiveUpdatePreference.set(context, appSettings.behavior.updateSavedCopies)
    }
    LaunchedEffect(appSettings.behavior.client.automation) {
        ThreadWatchScheduler
            .schedule(context, appSettings.behavior.client.automation)
    }
}

private fun mediaViewEvents(workspace: com.solthariel.pawchan.workspace.WorkspaceController) =
    com.solthariel.pawchan.media.MediaViewEvents { attachment ->
        workspace.recordUsage(
            if (attachment.viewer == com.solthariel.pawchan.media.AttachmentViewer.VIDEO) {
                com.solthariel.pawchan.domain.statistics.UsageMetric.VIDEOS_OPENED
            } else {
                com.solthariel.pawchan.domain.statistics.UsageMetric.IMAGES_OPENED
            },
            identity =
                com.solthariel.pawchan.domain.statistics
                    .usageMediaIdentity(attachment.originalUrl),
        )
    }
