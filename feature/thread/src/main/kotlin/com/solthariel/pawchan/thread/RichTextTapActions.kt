package com.solthariel.pawchan.thread

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.solthariel.pawchan.domain.model.PostLink

internal data class RichTextTapActions(
    val text: AnnotatedString,
    val onReference: (Long) -> Unit,
    val router: ((PostLink.Reference) -> Unit)?,
    val onExternal: (String) -> Unit,
    val onLinkLongPress: (PostLink) -> Boolean = { false },
)

internal fun richPostGestures(
    actions: RichTextTapActions,
    onSpoiler: (Int) -> Unit,
    onOffset: (Offset) -> Int?,
    onSelect: (TextRange) -> Unit,
    onLongPress: () -> Unit,
    feedback: PostLinkPressFeedback? = null,
): PostTextGestures =
    PostTextGestures(
        onTap = { position -> onOffset(position)?.let { handleRichTextTap(actions, it, onSpoiler) } },
        onDoubleTap = { position ->
            feedback?.release()
            onOffset(position)?.let { onSelect(paragraphRange(actions.text.text, it)) }
        },
        onLongPress = { position ->
            feedback?.release()
            val link = onOffset(position)?.let { richPostLinkAt(actions.text, it) }
            if (link == null || !actions.onLinkLongPress(link)) onLongPress()
        },
        onPress = { feedback?.press(it) },
        onRelease = { feedback?.release() },
        targetAt = { position ->
            onOffset(position)?.let { postTextTargetAt(actions.text, it) }
        },
        textKey = actions.text,
    )

internal fun showParagraphToolbar(
    context: Context,
    toolbar: TextToolbar,
    bounds: Rect,
    selected: String,
    onCopied: () -> Unit,
) {
    toolbar.showMenu(bounds, onCopyRequested = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Пост", selected))
        toolbar.hide()
        onCopied()
    })
}

private fun handleRichTextTap(
    actions: RichTextTapActions,
    offset: Int,
    onSpoiler: (Int) -> Unit,
) {
    val target = postTextTargetAt(actions.text, offset)
    when (target?.tag) {
        SPOILER_TAG -> {
            target.item.toIntOrNull()?.let(onSpoiler)
        }

        REFERENCE_TAG -> {
            parseReferenceToken(target.item)?.let {
                if (actions.router != null) actions.router.invoke(it) else actions.onReference(it.post.value)
            }
        }

        EXTERNAL_TAG -> {
            actions.onExternal(target.item)
        }
    }
}
