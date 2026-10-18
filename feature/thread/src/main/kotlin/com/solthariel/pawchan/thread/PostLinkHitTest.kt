package com.solthariel.pawchan.thread

import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.RemoteUrl

internal fun richPostLinkAt(
    text: AnnotatedString,
    offset: Int,
): PostLink? {
    val annotation = postLinkAnnotationAt(text, offset)
    return when (annotation?.tag) {
        REFERENCE_TAG -> parseReferenceToken(annotation.item)
        EXTERNAL_TAG -> RemoteUrl.parse(annotation.item)?.let(PostLink::External)
        else -> null
    }
}
