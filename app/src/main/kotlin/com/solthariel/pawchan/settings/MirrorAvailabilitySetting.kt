package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.infrastructure.LocalForumMirrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.solthariel.pawchan.R.string as L

@Composable
internal fun MirrorAvailabilitySetting(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
) {
    val res = LocalResources.current

    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    SettingsItemRow(
        SettingsItem.Toggle(
            res.getString(L.ui_auto_select_mirror),
            res.getString(L.ui_when_the_selected_one_is),
            app.behavior.client.connection.automaticFallback,
        ) { enabled ->
            update { it.withClient { c -> c.copy(connection = c.connection.copy(automaticFallback = enabled)) } }
        },
    )
    val mirrors = LocalForumMirrors.current ?: return
    val state by mirrors.state.collectAsStateWithLifecycle()
    val presentation by produceState(MirrorAvailabilityPresentation(), state.manualObservations) {
        value = withContext(Dispatchers.Default) { mirrorAvailabilityPresentation(state.manualObservations) }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    MirrorAvailabilityResults(presentation, state.manualChecking, mirrors::checkAvailability)
}
