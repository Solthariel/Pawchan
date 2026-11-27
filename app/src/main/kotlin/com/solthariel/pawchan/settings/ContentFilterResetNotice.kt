package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ContentFilterResetNotice(onDismiss: () -> Unit) {
    val res = LocalResources.current

    TransientNotice(res.getString(L.ui_saved_autohide_rules_were_reset), onDismiss = onDismiss)
}
