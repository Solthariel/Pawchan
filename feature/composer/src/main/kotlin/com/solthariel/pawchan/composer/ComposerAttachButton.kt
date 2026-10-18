package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerAttachButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(R.drawable.ic_composer_attach),
            contentDescription = stringResource(R.string.attachment_add),
            modifier = Modifier.size(26.dp),
        )
    }
}
