package com.solthariel.pawchan.composer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.OVAL_BUTTON_SHAPE
import com.solthariel.pawchan.core.ui.UiDisclosureIndicator
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.posting.PostingIcon
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun ComposerIconSelector(
    draft: ComposerDraftState,
    rules: PostingRules?,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
    compact: Boolean = false,
) {
    val res = LocalResources.current

    val icons = rules?.icons.orEmpty()
    if (icons.isEmpty()) return
    var expanded by remember(icons) { mutableStateOf(false) }
    val selected = icons.firstOrNull { it.id == draft.options.icon }
    val menuScroll = rememberScrollState()
    Box(if (compact) Modifier else Modifier.fillMaxWidth()) {
        if (compact) {
            val description =
                selected?.name?.let { res.getString(L.ui_post_icon, it) }
                    ?: res.getString(L.ui_choose_post_icon)
            IconButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier =
                    Modifier.size(APP_TOOLBAR_BUTTON_SIZE).semantics {
                        contentDescription = description
                        this.selected = selected != null
                    },
            ) {
                SelectedPostingIcon(selected)
            }
        } else {
            TextButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.name ?: res.getString(L.ui_no_icon), Modifier.weight(1f))
                UiDisclosureIndicator(expanded)
            }
        }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier =
                Modifier
                    .heightIn(max = ICON_MENU_MAX_HEIGHT)
                    .menuScrollbar(menuScroll, MaterialTheme.colorScheme.outline),
            scrollState = menuScroll,
        ) {
            ClearPostingIconItem(selected == null) {
                onChange(draft.copy(options = draft.options.copy(icon = null)))
                expanded = false
            }
            icons.forEach { icon ->
                PostingIconItem(icon, selected?.id == icon.id) {
                    onChange(draft.copy(options = draft.options.copy(icon = icon.id)))
                    expanded = false
                }
            }
        }
    }
}

@Composable
private fun PostingIconImage(icon: PostingIcon) {
    AsyncImage(
        model = icon.imageUrl,
        contentDescription = null,
        modifier = Modifier.size(width = 22.dp, height = 16.dp).clip(RoundedCornerShape(3.dp)),
        contentScale = ContentScale.Fit,
    )
}

private fun Modifier.menuScrollbar(
    scroll: ScrollState,
    color: Color,
): Modifier =
    drawWithContent {
        drawContent()
        if (scroll.maxValue <= 0) return@drawWithContent
        val viewportFraction = size.height / (size.height + scroll.maxValue)
        val thumbHeight = (size.height * viewportFraction).coerceAtLeast(ICON_MENU_MIN_THUMB.toPx())
        val progress = scroll.value.toFloat() / scroll.maxValue
        drawRoundRect(
            color = color,
            topLeft =
                Offset(
                    size.width - ICON_MENU_SCROLLBAR_WIDTH.toPx(),
                    (size.height - thumbHeight) * progress,
                ),
            size = Size(ICON_MENU_SCROLLBAR_WIDTH.toPx(), thumbHeight),
            cornerRadius = CornerRadius(ICON_MENU_SCROLLBAR_WIDTH.toPx()),
        )
    }

private val ICON_MENU_MAX_HEIGHT = 560.dp
private val ICON_MENU_MIN_THUMB = 36.dp
private val ICON_MENU_SCROLLBAR_WIDTH = 3.dp

@Composable
private fun SelectedPostingIcon(selected: PostingIcon?) {
    if (selected == null) {
        UiIcon(UiIconKind.DOCUMENT, MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp))
    } else {
        var loaded by remember(selected.imageUrl) { mutableStateOf(false) }
        Box {
            if (!loaded) {
                UiIcon(UiIconKind.DOCUMENT, MaterialTheme.colorScheme.primary, Modifier.size(24.dp))
            }
            AsyncImage(
                model = selected.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit,
                onSuccess = { loaded = true },
                onError = { loaded = false },
            )
        }
    }
}

@Composable
private fun ClearPostingIconItem(
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val res = LocalResources.current

    DropdownMenuItem(
        modifier = Modifier.clip(OVAL_BUTTON_SHAPE).semantics { selected = isSelected },
        text = { Text(res.getString(L.ui_no_icon)) },
        leadingIcon = { Box(Modifier.size(width = 22.dp, height = 16.dp)) },
        trailingIcon = { PostingIconSelectionMark(isSelected) },
        contentPadding = PaddingValues(horizontal = 16.dp),
        onClick = onClick,
    )
}

@Composable
private fun PostingIconItem(
    icon: PostingIcon,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = Modifier.clip(OVAL_BUTTON_SHAPE).semantics { selected = isSelected },
        text = { Text(icon.name) },
        leadingIcon = { PostingIconImage(icon) },
        trailingIcon = { PostingIconSelectionMark(isSelected) },
        contentPadding = PaddingValues(horizontal = 16.dp),
        onClick = onClick,
    )
}

@Composable
private fun PostingIconSelectionMark(isSelected: Boolean) {
    Box(Modifier.size(20.dp)) {
        if (isSelected) {
            UiIcon(UiIconKind.CHECK, MaterialTheme.colorScheme.primary, Modifier.size(20.dp))
        }
    }
}
