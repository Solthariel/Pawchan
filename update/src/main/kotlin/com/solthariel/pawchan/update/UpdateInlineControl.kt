package com.solthariel.pawchan.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Manual check, download and install remain distinct user actions. */
@Composable
public fun UpdateInlineControl(
    controller: UpdatePanelController,
    notice: @Composable (String?, Any) -> Unit,
) {
    val context = LocalContext.current
    val state by controller.model.state.collectAsState()
    var attempts by remember { mutableIntStateOf(0) }
    val label = stringResource(state.inlineLabel())
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        TextButton(
            onClick = {
                attempts++
                when (val current = state) {
                    is UpdateUiState.Available -> controller.model.download(current.release)
                    is UpdateUiState.Ready -> controller.model.install(context)
                    else -> controller.check()
                }
            },
            enabled = state !is UpdateUiState.Downloading && state != UpdateUiState.Checking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val progress = (state as? UpdateUiState.Downloading)?.progress
            Text(if (progress == null) label else stringResource(R.string.update_downloading, progress))
        }
        val downloading = state as? UpdateUiState.Downloading
        if (downloading != null) {
            LinearProgressIndicator(
                progress = { (downloading.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
    val message =
        when (val current = state) {
            UpdateUiState.UpToDate -> {
                stringResource(R.string.update_up_to_date)
            }

            is UpdateUiState.Error -> {
                val errorLabel =
                    if (current.reason == UpdateError.NOT_CONFIGURED) {
                        R.string.update_not_configured
                    } else {
                        R.string.update_error
                    }
                stringResource(errorLabel)
            }

            is UpdateUiState.Ready -> {
                if (current.permissionRequired) stringResource(R.string.update_permission) else null
            }

            else -> {
                null
            }
        }
    notice(message.takeIf { attempts > 0 }, state to attempts)
}

internal fun UpdateUiState.inlineLabel(): Int =
    when (this) {
        UpdateUiState.Checking -> R.string.update_checking
        is UpdateUiState.Available -> R.string.update_download
        is UpdateUiState.Downloading -> R.string.update_check
        is UpdateUiState.Ready -> R.string.update_install
        else -> R.string.update_check
    }
