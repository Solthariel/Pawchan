package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostLink

/** Prepared off the UI thread; the renderer is the same one used by forum posts. */
class PreparedPostText internal constructor(
    internal val value: RichPostTextUi,
)

fun preparePostText(
    document: PostDocument,
    originalPostId: Long? = null,
): PreparedPostText = PreparedPostText(RichPostTextUi.create(document, originalPostId))

data class SharedPostTextPresentation(
    val style: TextStyle,
    val maxLines: Int,
    val onTextLayout: (TextLayoutResult) -> Unit,
    val onLongPress: () -> Unit,
)

@Composable
fun SharedPostText(
    model: PreparedPostText,
    presentation: SharedPostTextPresentation,
    onReference: (PostLink.Reference) -> Unit,
) {
    CompositionLocalProvider(LocalPostPreview provides onReference) {
        RichPostText(
            model.value,
            onPostReference = {},
            presentation =
                RichPostPresentation(
                    style = presentation.style,
                    maxLines = presentation.maxLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = presentation.onTextLayout,
                    onLongPress = presentation.onLongPress,
                ),
        )
    }
}
