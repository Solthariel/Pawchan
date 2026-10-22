package com.solthariel.pawchan.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Presentation owned by the caller's ViewModelStore; no APK is downloaded by automatic checks. */
public class UpdatePanelController internal constructor(
    internal val model: UpdateViewModel,
    internal val configuration: UpdateConfiguration,
)

/** Kept above lazy settings rows, so discovery does not depend on scrolling the panel into view. */
@Composable
public fun rememberUpdatePanel(
    configuration: UpdateConfiguration,
    checkRequest: UpdateCheckRequest? = null,
    onUpdateAvailable: (Long) -> Unit = {},
    dns: okhttp3.Dns = okhttp3.Dns.SYSTEM,
): UpdatePanelController {
    val context = LocalContext.current
    val factory = remember(context, configuration, dns) { UpdateViewModel.factory(context, configuration, dns) }
    val model: UpdateViewModel =
        viewModel(
            key = "update-${configuration.currentVersionCode}-${configuration.manifestUrl}",
            factory = factory,
        )
    val notifyAvailable by rememberUpdatedState(onUpdateAvailable)
    LaunchedEffect(model, checkRequest) { model.checkOnEntry(checkRequest, configuration) }
    LaunchedEffect(model, checkRequest) {
        if (checkRequest == null) return@LaunchedEffect
        model.state
            .map { (it as? UpdateUiState.Available)?.release?.versionCode }
            .distinctUntilChanged()
            .collect { it?.let(notifyAvailable) }
    }
    return remember(model, configuration) { UpdatePanelController(model, configuration) }
}
