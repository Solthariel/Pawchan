package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.update.UpdateInlineControl
import com.solthariel.pawchan.update.UpdatePanelController

@Composable
internal fun AboutUpdateRow(controller: UpdatePanelController) {
    UpdateInlineControl(controller) { message, event -> TransientNotice(message, eventKey = event) }
}
