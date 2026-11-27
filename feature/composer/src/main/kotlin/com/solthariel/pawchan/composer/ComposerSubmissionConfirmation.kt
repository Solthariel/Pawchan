package com.solthariel.pawchan.composer

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun confirmedCaptchaReload(
    failure: ComposerFailureState,
    onReload: () -> Unit,
): () -> Unit {
    val res = LocalResources.current

    var confirming by remember(failure.error) { mutableStateOf(false) }
    if (confirming && failure.error == ReplyError.SUBMISSION_UNCONFIRMED) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(res.getString(L.ui_did_you_check_the_submission)) },
            text = {
                Text(
                    res.getString(L.ui_the_server_may_have_accepted) +
                        res.getString(L.ui_retrying_may_create_a_duplicate) +
                        res.getString(L.ui_continuing_will_only_load_the),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onReload()
                }) { Text(res.getString(L.ui_prepare_retry)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(res.getString(L.ui_cancel)) }
            },
        )
    }
    return {
        if (failure.error == ReplyError.SUBMISSION_UNCONFIRMED) confirming = true else onReload()
    }
}
