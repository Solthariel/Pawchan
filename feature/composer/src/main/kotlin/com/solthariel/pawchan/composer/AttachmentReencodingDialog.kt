package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.feature.composer.R

/** Apply commits the draft; Reset clears it without closing; Cancel/Back discard pending edits. */
@Composable
internal fun AttachmentReencodingDialog(
    file: ComposerAttachment,
    enabled: Boolean,
    onUpdate: (ComposerAttachment) -> Unit,
    onDismiss: () -> Unit,
) {
    var settings by remember(file.id) {
        mutableStateOf(file.options.reencoding ?: file.options.previousReencoding ?: ImageReencoding())
    }
    AppDialog(onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(AppDialogStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.attachment_reencoding_settings),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            AttachmentReencodingControls(settings, enabled) { settings = it }
            AttachmentReencodingActions(
                enabled = enabled,
                onDismiss = onDismiss,
                onReset = {
                    settings = ImageReencoding()
                    onUpdate(file.copy(options = file.options.copy(reencoding = null, previousReencoding = null)))
                },
                onConfirm = {
                    onUpdate(
                        file.copy(
                            options =
                                file.options.copy(
                                    reencoding = settings,
                                    previousReencoding = settings,
                                ),
                        ),
                    )
                    onDismiss()
                },
            )
        }
    }
}
