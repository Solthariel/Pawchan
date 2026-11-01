package com.solthariel.pawchan.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
public fun UpdatePanel(
    controller: UpdatePanelController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val updateViewModel = controller.model
    val configuration = controller.configuration
    val state by updateViewModel.state.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.update_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        UpdateVersionSummary(
            installedVersion = configuration.currentVersionName,
            availableVersion = state.availableVersionName(),
        )
        UpdateStatus(
            state = state,
            onCheck = { updateViewModel.check(configuration) },
            onDownload = updateViewModel::download,
            onInstall = { updateViewModel.install(context) },
        )
    }
}

@Composable
private fun UpdateStatus(
    state: UpdateUiState,
    onCheck: () -> Unit,
    onDownload: (UpdatePackage) -> Unit,
    onInstall: () -> Unit,
) {
    when (state) {
        UpdateUiState.Idle -> {
            ActionButton(R.string.update_check, onCheck)
        }

        UpdateUiState.Checking -> {
            Text(stringResource(R.string.update_checking))
        }

        UpdateUiState.UpToDate -> {
            Text(stringResource(R.string.update_up_to_date))
            ActionButton(R.string.update_check, onCheck)
        }

        is UpdateUiState.Available -> {
            AvailableUpdate(state, onDownload)
        }

        is UpdateUiState.Downloading -> {
            DownloadProgress(state.progress)
        }

        is UpdateUiState.Ready -> {
            ReadyUpdate(state, onInstall)
        }

        is UpdateUiState.Error -> {
            ErrorStatus(state.reason, onCheck)
        }
    }
}

@Composable
private fun AvailableUpdate(
    state: UpdateUiState.Available,
    onDownload: (UpdatePackage) -> Unit,
) {
    state.changelog?.takeIf(String::isNotBlank)?.let { changelog ->
        ReleaseNotesContent(changelog)
    }
    ActionButton(R.string.update_download) { onDownload(state.release) }
}

@Composable
private fun DownloadProgress(progress: Int) {
    Text(stringResource(R.string.update_downloading, progress))
    LinearProgressIndicator(
        progress = { progress / PERCENT_DIVISOR },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReadyUpdate(
    state: UpdateUiState.Ready,
    onInstall: () -> Unit,
) {
    Text(stringResource(R.string.update_ready))
    if (state.permissionRequired) {
        Text(
            text = stringResource(R.string.update_permission),
            color = MaterialTheme.colorScheme.error,
        )
    }
    ActionButton(R.string.update_install, onInstall)
}

@Composable
private fun ErrorStatus(
    reason: UpdateError,
    onCheck: () -> Unit,
) {
    val message =
        when (reason) {
            UpdateError.NOT_CONFIGURED -> R.string.update_not_configured
            UpdateError.GENERAL -> R.string.update_error
        }
    Text(
        text = stringResource(message),
        color = MaterialTheme.colorScheme.error,
    )
    ActionButton(R.string.update_check, onCheck)
}

@Composable
private fun ActionButton(
    label: Int,
    onClick: () -> Unit,
) {
    Button(onClick = onClick) {
        Text(stringResource(label))
    }
}

private const val PERCENT_DIVISOR = 100f
