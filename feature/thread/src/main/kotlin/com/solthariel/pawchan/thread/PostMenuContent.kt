package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.solthariel.pawchan.core.ui.ActionMenuItem

internal enum class PostMenuPage { MAIN, COPY, HIDE }

internal enum class PostMenuCommand {
    REPLY,
    QUOTE,
    COPY_TEXT,
    COPY_MARKUP,
    COPY_LINK,
    REPORT,
    OWN,
    HIDE,
    HIDE_TREE,
    HIDE_NAME,
    HIDE_SIMILAR,
    SHOW,
    SHOW_ALL,
    ADD_FILTER,
}

internal data class PostMenuOptions(
    val hasText: Boolean,
    val closed: Boolean,
    val canReport: Boolean,
    val own: Boolean,
    val hidden: Boolean,
    val hasLink: Boolean = false,
)

@Composable
internal fun PostMenuContent(
    page: PostMenuPage,
    options: PostMenuOptions,
    onPage: (PostMenuPage) -> Unit,
    onCommand: (PostMenuCommand) -> Unit,
    canAddRule: Boolean = false,
) {
    when (page) {
        PostMenuPage.MAIN -> {
            PostMenuMain(options, onPage, onCommand, canAddRule)
        }

        PostMenuPage.COPY -> {
            if (options.hasText) {
                ActionMenuItem("Текст", { onCommand(PostMenuCommand.COPY_TEXT) })
                ActionMenuItem("Разметку", { onCommand(PostMenuCommand.COPY_MARKUP) })
            }
            ActionMenuItem("Ссылку", { onCommand(PostMenuCommand.COPY_LINK) }, options.hasLink)
        }

        PostMenuPage.HIDE -> {
            ActionMenuItem("Этот пост", { onCommand(PostMenuCommand.HIDE) })
            ActionMenuItem("Дерево ответов", { onCommand(PostMenuCommand.HIDE_TREE) })
            ActionMenuItem("Посты с тем же именем", { onCommand(PostMenuCommand.HIDE_NAME) })
            ActionMenuItem("Похожие посты", { onCommand(PostMenuCommand.HIDE_SIMILAR) }, options.hasText)
        }
    }
}

@Composable
private fun PostMenuMain(
    options: PostMenuOptions,
    onPage: (PostMenuPage) -> Unit,
    onCommand: (PostMenuCommand) -> Unit,
    canAddRule: Boolean,
) {
    ActionMenuItem("Ответить", { onCommand(PostMenuCommand.REPLY) }, !options.closed)
    ActionMenuItem("Цитировать", { onCommand(PostMenuCommand.QUOTE) }, !options.closed && options.hasText)
    ActionMenuItem("Копировать ›", { onPage(PostMenuPage.COPY) })
    ActionMenuItem("Пожаловаться", { onCommand(PostMenuCommand.REPORT) }, options.canReport)
    TextButton(onClick = { onCommand(PostMenuCommand.OWN) }, modifier = Modifier.fillMaxWidth()) {
        Text("Моё сообщение", Modifier.weight(1f))
        Checkbox(options.own, onCheckedChange = null)
    }
    if (canAddRule) ActionMenuItem("Добавить правило", { onCommand(PostMenuCommand.ADD_FILTER) })
    if (!options.hidden) {
        ActionMenuItem("Скрыть ›", { onPage(PostMenuPage.HIDE) })
    }
}
