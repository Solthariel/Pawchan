package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class PostMenuEnvironment(
    val tools: ThreadToolsState,
    val actions: ThreadActions,
    val presentation: ThreadPresentation,
    val context: ThreadPostContext,
    val addFilter: ((ContentFilterSelection) -> Unit)? = null,
)

internal suspend fun executePostMenu(
    command: PostMenuCommand,
    post: ThreadPostUi,
    environment: PostMenuEnvironment,
    platform: PostMenuTransfer,
) {
    if (command == PostMenuCommand.SHOW && post.metadata.contentFilterLabel != null) {
        environment.context.revealContentFilter(post.id)
        return
    }
    val preference = command.preferenceAction()
    if (preference != null) {
        changePostPreference(preference, command, post, environment)
    } else {
        when (command) {
            PostMenuCommand.ADD_FILTER -> {
                environment.addFilter?.invoke(post.contentFilterSelection(environment.context.address))
            }

            PostMenuCommand.REPLY -> {
                environment.actions.onReplyTo(">>${post.id}\n")
            }

            PostMenuCommand.QUOTE -> {
                val text = withContext(Dispatchers.Default) { postQuote(post) }
                environment.actions.onReplyTo(text)
            }

            PostMenuCommand.REPORT -> {
                environment.presentation.reports?.reset()
                environment.tools.reportTargets = setOf(post.id)
                environment.tools.dialog = ThreadDialog.REPORT
            }

            else -> {
                copyPost(command, post, environment, platform)
            }
        }
    }
}

private suspend fun changePostPreference(
    preference: PostPreferenceAction,
    command: PostMenuCommand,
    post: ThreadPostUi,
    environment: PostMenuEnvironment,
) {
    if (!environment.context.preferences.change(preference, post)) {
        environment.tools.notice = "Не удалось сохранить отметку или правило скрытия"
    } else if (command == PostMenuCommand.OWN) {
        post.address(environment)?.let { environment.context.storage?.own(it, !post.metadata.changes.own) }
    }
}

private suspend fun copyPost(
    command: PostMenuCommand,
    post: ThreadPostUi,
    environment: PostMenuEnvironment,
    platform: PostMenuTransfer,
) {
    val text =
        withContext(Dispatchers.Default) {
            when (command) {
                PostMenuCommand.COPY_MARKUP -> {
                    post.document.toPostingMarkup()
                }

                PostMenuCommand.COPY_LINK -> {
                    platform.postLink(environment.context.address, post.id)
                }

                else -> {
                    post.document.plainText
                }
            }
        } ?: return
    platform.copy(text)
    environment.tools.notice = "Скопировано"
}

private fun PostMenuCommand.preferenceAction(): PostPreferenceAction? =
    when (this) {
        PostMenuCommand.OWN -> PostPreferenceAction.OWN
        PostMenuCommand.HIDE -> PostPreferenceAction.HIDE
        PostMenuCommand.HIDE_TREE -> PostPreferenceAction.HIDE_TREE
        PostMenuCommand.HIDE_NAME -> PostPreferenceAction.HIDE_NAME
        PostMenuCommand.HIDE_SIMILAR -> PostPreferenceAction.HIDE_SIMILAR
        PostMenuCommand.SHOW -> PostPreferenceAction.SHOW
        PostMenuCommand.SHOW_ALL -> PostPreferenceAction.SHOW_ALL
        else -> null
    }

private fun ThreadPostUi.address(environment: PostMenuEnvironment): PostAddress? =
    PostId.parse(id)?.let { PostAddress(environment.context.address, it) }

internal fun ThreadPostUi.contentFilterSelection(address: ThreadAddress): ContentFilterSelection =
    ContentFilterSelection(address.board, document.plainText, id != address.thread.value)
