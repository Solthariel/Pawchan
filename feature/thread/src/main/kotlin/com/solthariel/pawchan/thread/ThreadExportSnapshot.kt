package com.solthariel.pawchan.thread

/** HTML keeps the original post markup; the PNG path never reconstructs a header or body from this document. */
internal fun ThreadUiState.Content.exportDocument(): ThreadExportDocument =
    ThreadExportDocument(
        id,
        subject,
        posts.map { post ->
            ThreadExportPost(
                post.metadata.display.index,
                post.metadata.display.header,
                post.metadata.display.subject,
                post.document,
                post.attachments.toList(),
            )
        },
    )
