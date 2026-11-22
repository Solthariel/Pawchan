package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.solthariel.pawchan.domain.model.PostDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The standard post menu with unsupported source capabilities disabled or omitted. */
@Composable
fun ReadOnlyPostMenu(
    document: PostDocument,
    transfer: PostMenuTransfer,
    link: String? = null,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    PostMenuDialog(
        options =
            PostMenuOptions(
                hasText = document.plainText.isNotBlank(),
                hasLink = link != null,
                closed = true,
                canReport = false,
                own = false,
                hidden = false,
                storageActions = false,
            ),
        onCommand = { command ->
            scope.launch {
                val text =
                    withContext(Dispatchers.Default) {
                        when (command) {
                            PostMenuCommand.COPY_TEXT -> document.plainText
                            PostMenuCommand.COPY_MARKUP -> document.toPostingMarkup()
                            PostMenuCommand.COPY_LINK -> link
                            else -> null
                        }
                    }
                if (text != null) transfer.copy(text)
                onClose()
            }
        },
        onClose = onClose,
    )
}
