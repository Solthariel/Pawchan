package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerAttachButton(
    enabled: Boolean = true,
    preserveAppearance: Boolean = false,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.size(APP_TOOLBAR_BUTTON_SIZE),
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor =
                    if (preserveAppearance) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        IconButtonDefaults.filledIconButtonColors().disabledContainerColor
                    },
                disabledContentColor =
                    if (preserveAppearance) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        IconButtonDefaults.filledIconButtonColors().disabledContentColor
                    },
            ),
    ) {
        Icon(
            painterResource(R.drawable.ic_composer_attach),
            contentDescription = stringResource(R.string.attachment_add),
            modifier = Modifier.size(24.dp),
        )
    }
}
