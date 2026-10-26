package com.solthariel.pawchan.thread

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.solthariel.pawchan.domain.model.ThreadAddress

internal data class ReplyReferenceContext(
    val address: ThreadAddress,
    val postId: Long,
)

/** Decorates prepared UI text only: content, link annotations and spoiler markers are unchanged. */
internal fun AnnotatedString.highlightReplyReference(context: ReplyReferenceContext?): AnnotatedString {
    if (context == null) return this
    val builder = AnnotatedString.Builder(this)
    getStringAnnotations(REFERENCE_TAG, 0, length).forEach { annotation ->
        val reference = parseReferenceToken(annotation.item)
        if (reference?.post?.value == context.postId &&
            (reference.thread == null || reference.thread == context.address)
        ) {
            builder.addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), annotation.start, annotation.end)
        }
    }
    return builder.toAnnotatedString()
}
