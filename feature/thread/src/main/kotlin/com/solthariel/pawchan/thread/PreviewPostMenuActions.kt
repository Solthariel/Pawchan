package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId

/** Menu semantics stay in the feature; app effects receive only addressed domain data. */
internal fun executePreviewPostMenu(
    command: PostMenuCommand,
    post: ThreadPostUi,
    content: ThreadUiState.Content,
    actions: PostPreviewActions,
    transfer: PostMenuTransfer,
    onReply: () -> Unit,
) {
    val address = content.address ?: return
    when (command) {
        PostMenuCommand.REPLY, PostMenuCommand.QUOTE -> {
            actions.prepareReply(
                address,
                if (command ==
                    PostMenuCommand.REPLY
                ) {
                    ">>${post.id}\n"
                } else {
                    selectedPostQuote(post.id, post.document.plainText)
                },
            )
            onReply()
            actions.navigate(address)
        }

        PostMenuCommand.COPY_TEXT, PostMenuCommand.COPY_MARKUP -> {
            transfer.copy(
                if (command ==
                    PostMenuCommand.COPY_TEXT
                ) {
                    post.document.plainText
                } else {
                    post.document.toPostingMarkup()
                },
            )
        }

        else -> {
            transfer.postLink(address, post.id)?.let(transfer::copy)
        }
    }
}
