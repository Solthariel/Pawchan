package com.solthariel.pawchan.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.infrastructure.AppNetworkStack
import com.solthariel.pawchan.infrastructure.AppNetworkViewModel
import com.solthariel.pawchan.infrastructure.AppPreferencesViewModel
import com.solthariel.pawchan.infrastructure.AppStorageViewModel
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
    loaded?.let { initial -> LoadedPawchanApp(settings, preferenceOwner.settings, initial) }
}

@Composable
private fun LoadedPawchanApp(
    settings: SettingsViewModel,
    repository: DataStoreSettingsRepository,
    initial: AppSettings,
) {
    val context = LocalContext.current
    val network: AppNetworkViewModel =
        viewModel(
            factory =
                AppNetworkViewModel.factory(
                    context,
                    repository,
                    initial.behavior.client.storage.cacheMb,
                    initial.behavior.client.mirror,
                    initial.behavior.dns.github,
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

    AppSessionSurface(appSettings, storage, dependencies, archives, settings, network.stack.github)
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
    PawchanTheme(preference = theme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(Modifier.fillMaxSize()) {
                if (workspaceReady) {
                    home?.let { loaded ->
                        CompositionLocalProvider(
                            com.solthariel.pawchan.infrastructure.LocalGithubNetwork provides github,
                            com.solthariel.pawchan.infrastructure.sites.LocalForumMirror provides
                                appSettings.behavior.client.mirror,
                            LocalPostDisplayPreferences provides rememberPhonePostDisplay(appSettings),
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

/** Настройки долгоживущих сервисов: видео, обновление копий, DNS внешних сервисов. */
@Composable
private fun BindAppSettingsEffects(
    appSettings: AppSettings,
    archives: ArchiveController,
    loaders: MediaLoaders,
    stack: AppNetworkStack,
) {
    androidx.compose.runtime.SideEffect { stack.selectMirror(appSettings.behavior.client.mirror) }
    val context = LocalContext.current
    androidx.compose.runtime.SideEffect {
        stack.github.update(appSettings.behavior.dns.github)
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
    LaunchedEffect(loaders, appSettings.behavior.dns.arhivach) {
        loaders.arhivachDns?.update(appSettings.behavior.dns.arhivach)
    }
    LaunchedEffect(loaders, appSettings.behavior.dns.vocaroo) {
        loaders.vocaroo?.update(appSettings.behavior.dns.vocaroo)
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
