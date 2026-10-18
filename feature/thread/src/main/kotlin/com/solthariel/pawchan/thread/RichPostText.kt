package com.solthariel.pawchan.thread

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.thread.vocaroo.rememberVocarooInlineContent

@Composable
internal fun RichPostText(
    model: RichPostTextUi,
    onPostReference: (Long) -> Unit,
    modifier: Modifier = Modifier,
    presentation: RichPostPresentation = RichPostPresentation(),
    /** Номер поста: раскрытые спойлеры хранятся на уровне треда и переживают обновление. */
    spoilerKey: Long? = null,
) {
    val store = LocalRevealedSpoilers.current?.takeIf { spoilerKey != null }
    var ownSpoilers by rememberSaveable(model, stateSaver = SpoilerSetSaver) { mutableStateOf(emptySet<Int>()) }
    val spoilers = if (store != null && spoilerKey != null) store[spoilerKey] else ownSpoilers
    val text = remember(model, spoilers) { if (spoilers.isEmpty()) model.initialText else model.reveal(spoilers) }
    val density = LocalDensity.current.density
    val feedback = remember(text, density) { PostLinkPressFeedback(text, LINK_TOUCH_PADDING_DP * density) }
    val selection = rememberPostTextSelection(model, text.text)
    val field = TextFieldValue(text, selection.range)
    val uri = LocalUriHandler.current
    val router = LocalPostPreview.current
    val address = LocalPostLinkAddress.current
    val links = LocalPostLinks.current
    val resolver = links?.resolver
    val opener = PostLinkOpener(resolver, address, router, links, LocalPostLinkMedia.current, uri::openUri)
    var linkMenu by remember(model) { mutableStateOf<PostLinkTarget?>(null) }
    val gestures =
        richPostGestures(
            RichTextTapActions(text, onPostReference, router, opener::open, onLinkLongPress = { link: PostLink ->
                val target = resolver?.resolve(link, address)
                linkMenu = target
                target != null
            }),
            onSpoiler = { index ->
                if (store != null && spoilerKey != null) store.reveal(spoilerKey, index) else ownSpoilers += index
            },
            onOffset = feedback::offsetAt,
            onSelect = selection::select,
            onLongPress = { presentation.onLongPress?.invoke() },
            feedback = feedback,
        )
    RichPostBody(
        text,
        selection,
        presentation,
        modifier
            .postLinkPressHighlight(feedback, MaterialTheme.colorScheme.primary.copy(alpha = LINK_PRESS_ALPHA))
            .onGloballyPositioned { selection.bounds = it.boundsInRoot() }
            .postTextGestures(model, !field.selection.collapsed, gestures),
    ) {
        feedback.updateLayout(it)
        presentation.onTextLayout?.invoke(it)
    }
    PostLinkMenu(linkMenu) { linkMenu = null }
}

@Composable
private fun RichPostBody(
    text: AnnotatedString,
    selection: PostTextSelection,
    presentation: RichPostPresentation,
    modifier: Modifier,
    onLayout: (TextLayoutResult) -> Unit,
) {
    val visual = remember(text) { text.withVisualLinkSuffixes() }
    val layout = remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val field = TextFieldValue(visual, selection.range)
    val styled = presentation.style ?: MaterialTheme.typography.bodyMedium
    val measurer = rememberTextMeasurer()
    val interactive =
        modifier.referenceLinkSuffixes(text, LocalPostLinkAddress.current, measurer, styled) { layout.value }
    val inline = rememberVocarooInlineContent(text, measurer, styled)
    if (!presentation.interactionsEnabled || field.selection.collapsed) {
        Text(
            visual,
            interactive,
            style = styled,
            inlineContent = inline,
            maxLines = presentation.maxLines,
            overflow = presentation.overflow,
            onTextLayout = {
                layout.value = it
                onLayout(it)
            },
        )
    } else {
        SelectablePostField(
            field,
            presentation,
            styled,
            interactive
                .focusRequester(selection.focus)
                .onFocusChanged {
                    if (!it.isFocused && selection.focused) selection.clear()
                    selection.focused = it.isFocused
                },
            onChange = { selection.select(it.selection) },
            onLayout = {
                layout.value = it
                onLayout(it)
            },
        )
    }
}

internal data class RichPostPresentation(
    val style: TextStyle? = null,
    val maxLines: Int = Int.MAX_VALUE,
    val overflow: TextOverflow = TextOverflow.Clip,
    val interactionsEnabled: Boolean = true,
    val onLongPress: (() -> Unit)? = null,
    val onQuoteSelection: ((String) -> Unit)? = null,
    val onTextLayout: ((TextLayoutResult) -> Unit)? = null,
)

private data object QuoteSelectionKey

@Composable
private fun SelectablePostField(
    field: TextFieldValue,
    presentation: RichPostPresentation,
    styled: TextStyle,
    modifier: Modifier,
    onChange: (TextFieldValue) -> Unit,
    onLayout: (TextLayoutResult) -> Unit,
) {
    BasicTextField(
        value = field,
        onValueChange = onChange,
        readOnly = true,
        textStyle =
            styled.copy(
                color =
                    if (styled.color ==
                        androidx.compose.ui.graphics.Color.Unspecified
                    ) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        styled.color
                    },
            ),
        cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
        maxLines = presentation.maxLines,
        modifier =
            modifier
                .appendTextContextMenuComponents {
                    presentation.onQuoteSelection?.let { quote ->
                        if (!field.selection.collapsed) {
                            item(QuoteSelectionKey, "Цитировать") {
                                quote(field.text.substring(field.selection.min, field.selection.max))
                                onChange(field.copy(selection = androidx.compose.ui.text.TextRange.Zero))
                                close()
                            }
                        }
                    }
                },
        onTextLayout = onLayout,
    )
}

private val SpoilerSetSaver =
    Saver<Set<Int>, ArrayList<Int>>(
        save = { ArrayList(it) },
        restore = { it.toSet() },
    )
