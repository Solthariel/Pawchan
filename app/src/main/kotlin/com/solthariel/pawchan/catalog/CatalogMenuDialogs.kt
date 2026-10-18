package com.solthariel.pawchan.catalog

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.solthariel.pawchan.R
import com.solthariel.pawchan.domain.boards.BoardInformation
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.isRateLimited
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun CatalogMenuDialogs(menu: CatalogMenuBindings) {
    val context = LocalContext.current
    val saveFailed = menu.state.preferences.saveFailed
    LaunchedEffect(saveFailed) {
        if (saveFailed) {
            Toast.makeText(context, R.string.board_save_failed, Toast.LENGTH_SHORT).show()
            menu.onDismiss()
        }
    }
    val info = menu.state.information
    if (info != BoardInformationState.Closed) {
        AlertDialog(
            onDismissRequest = menu.onDismiss,
            title = { Text(stringResource(R.string.board_information)) },
            text = { InformationDialogBody(info) },
            confirmButton = {},
        )
    }
}

@Composable
private fun InformationDialogBody(state: BoardInformationState) {
    when (state) {
        BoardInformationState.Loading -> {
            Text(stringResource(R.string.board_information_loading))
        }

        is BoardInformationState.Content -> {
            BoardInformationBody(state.information)
        }

        is BoardInformationState.Error -> {
            Text(
                stringResource(
                    if (state.failure?.isRateLimited == true) {
                        R.string.board_rate_limited
                    } else {
                        R.string.board_information_failed
                    },
                ),
            )
        }

        BoardInformationState.Closed -> {
            Unit
        }
    }
}

@Composable
private fun BoardInformationBody(info: BoardInformation) {
    // AppAlertDialog already scrolls its text slot.
    Column {
        Text("/${info.address.board.value}/ — ${info.title}")
        Text(info.description.ifBlank { stringResource(R.string.board_description_missing) })
        info.defaultName?.let { Text(stringResource(R.string.board_default_name, it)) }
        info.bumpLimit?.let { Text(stringResource(R.string.board_bump_limit, it)) }
        info.maxCommentLength?.let { Text(stringResource(R.string.board_comment_limit, it)) }
        if (info.configuration.subjectsEnabled != null || info.configuration.namesEnabled != null) {
            Text(stringResource(R.string.board_configuration))
            info.configuration.subjectsEnabled?.let {
                Text(stringResource(if (it) R.string.board_subjects_enabled else R.string.board_subjects_disabled))
            }
            info.configuration.namesEnabled?.let {
                Text(stringResource(if (it) R.string.board_names_enabled else R.string.board_names_disabled))
            }
        }
    }
}
