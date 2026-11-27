package com.solthariel.pawchan.infrastructure

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.domain.network.ForumMirrors
import com.solthariel.pawchan.infrastructure.sites.LocalForumMirror
import com.solthariel.pawchan.R.string as L

internal val LocalForumMirrors = staticCompositionLocalOf<ForumMirrors?> { null }

@Composable
internal fun MirrorSessionHost(
    stack: AppNetworkStack,
    content: @Composable () -> Unit,
) {
    val res = LocalResources.current

    val recovery = stack.mirror.recovery
    if (recovery == null) {
        content()
        return
    }
    val state by recovery.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(stack, state.activeHost) { stack.selectMirror(state.activeHost) }
    LaunchedEffect(state.switchSerial) {
        if (state.switchSerial > 0L) {
            Toast
                .makeText(
                    context,
                    res.getString(L.ui_mirror_unavailable_switched_to, state.activeHost),
                    Toast.LENGTH_LONG,
                ).show()
        }
    }
    CompositionLocalProvider(
        LocalForumMirrors provides recovery,
        LocalForumMirror provides state.activeHost,
    ) { content() }
}
