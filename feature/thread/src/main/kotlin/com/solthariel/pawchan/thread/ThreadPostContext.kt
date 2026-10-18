package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.launch

internal data class ThreadPostContext(
    val address: ThreadAddress,
    val preferences: ThreadPostPreferencesController,
    val storage: ThreadPostStorage? = null,
    val revealContentFilter: (Long) -> Unit = {},
)

@Composable
internal fun ThreadPostMenuHost(
    content: ThreadUiState.Content,
    tools: ThreadToolsState,
    actions: ThreadActions,
    presentation: ThreadPresentation,
) {
    val post =
        tools.postMenuId
            ?.let { content.presentation.postPositions[it] }
            ?.let { content.posts[it] } ?: return
    val postContext = presentation.postContext
    val bindings = LocalPostLinks.current
    if (postContext == null || bindings == null) return
    val links = bindings.resolver
    val platform = bindings.transfer
    val scope = rememberCoroutineScope()
    val addFilter = LocalAddContentFilter.current
    val environment = PostMenuEnvironment(tools, actions, presentation, postContext, addFilter)
    PostMenuDialog(
        PostMenuOptions(
            post.document.plainText.isNotBlank(),
            content.closed,
            presentation.reports?.available == true,
            post.metadata.changes.own,
            post.metadata.hidden,
            links.postUrl(postContext.address, post.id) != null,
        ),
        canAddRule = addFilter != null,
        onCommand = { command ->
            scope.launch {
                executePostMenu(command, post, environment, platform)
                tools.postMenuId = null
            }
        },
        onClose = { tools.postMenuId = null },
    )
}
