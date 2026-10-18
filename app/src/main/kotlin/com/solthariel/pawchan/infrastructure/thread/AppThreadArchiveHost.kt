package com.solthariel.pawchan.infrastructure.thread

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.archive.ArhivachController
import com.solthariel.pawchan.archive.ArhivachSubmitDialog
import com.solthariel.pawchan.archive.ThreadArchiveDialog
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.thread.ThreadArchiveHost
import com.solthariel.pawchan.thread.ThreadExternalArchive
import com.solthariel.pawchan.thread.ThreadExternalArchiveState
import com.solthariel.pawchan.workspace.LocalWorkspace

@Composable
internal fun rememberAppThreadArchiveHost(): ThreadArchiveHost {
    val context = LocalContext.current
    val arhivach = LocalWorkspace.current?.archives?.arhivach
    return remember(context, arhivach) {
        ThreadArchiveHost(
            copyDialog = { address, close -> ThreadArchiveDialog(address, close) },
            external =
                arhivach?.let { controller ->
                    ThreadExternalArchive(
                        state = { address -> rememberArhivachState(controller, address) },
                        submitDialog = { address, close -> ArhivachSubmitDialog(controller, address, close) },
                        open = { link -> context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri())) },
                        load = controller::load,
                    )
                },
        )
    }
}

@Composable
private fun rememberArhivachState(
    controller: ArhivachController,
    address: ThreadAddress,
): ThreadExternalArchiveState {
    LaunchedEffect(controller) { controller.refreshAvailability() }
    val links = controller.links.collectAsStateWithLifecycle().value
    val available = controller.available.collectAsStateWithLifecycle().value
    return ThreadExternalArchiveState(links[address], available)
}
