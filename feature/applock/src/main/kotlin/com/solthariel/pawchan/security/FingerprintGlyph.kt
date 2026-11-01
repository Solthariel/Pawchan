package com.solthariel.pawchan.security

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.solthariel.pawchan.feature.applock.R

/** Decorative Rounded glyph; the parent retains input, accessibility and authentication. */
@Composable
internal fun FingerprintGlyph(modifier: Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_applock_fingerprint),
        contentDescription = null,
        tint = LocalContentColor.current,
        modifier = modifier,
    )
}
