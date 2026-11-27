package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.R.string as L

/** Filled down/up triangles expose the actual disclosure state. */
@Composable
fun UiDisclosureIndicator(
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current

    Icon(
        painterResource(
            if (expanded) R.drawable.ic_rounded_arrow_drop_up else R.drawable.ic_rounded_arrow_drop_down,
        ),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .size(NAVIGATION_INDICATOR_SIZE)
                .testTag("disclosure-indicator")
                .semantics {
                    stateDescription =
                        if (expanded) {
                            res.getString(
                                L.ui_expanded,
                            )
                        } else {
                            res.getString(L.ui_collapsed)
                        }
                },
    )
}

/** A chevron denotes navigation to a destination. */
@Composable
fun UiForwardIndicator(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_rounded_chevron_right),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(NAVIGATION_INDICATOR_SIZE).testTag("forward-indicator"),
    )
}

@Composable
fun UiBackwardIndicator(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_rounded_arrow_back),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(NAVIGATION_INDICATOR_SIZE).testTag("backward-indicator"),
    )
}

/** A filled triangle denotes a submenu, not a destination. */
@Composable
fun UiSubmenuIndicator(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_rounded_arrow_right),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(NAVIGATION_INDICATOR_SIZE).testTag("submenu-indicator"),
    )
}

private val NAVIGATION_INDICATOR_SIZE = 20.dp
