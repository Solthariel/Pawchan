package com.solthariel.pawchan.thread

/** Refresh may update an open preview, but losing its source must not erase an already visible post. */
internal fun retainPreview(
    current: PostPreviewState,
    replacement: PostPreviewState,
): PostPreviewState =
    if (current.content != null && current.posts.isNotEmpty() && replacement.posts.isEmpty()) {
        current
    } else {
        replacement
    }
