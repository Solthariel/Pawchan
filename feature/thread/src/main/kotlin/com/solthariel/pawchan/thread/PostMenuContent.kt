package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.MenuNavigation
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiMenuIcon

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
    val storageActions: Boolean = true,
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
                ActionMenuItem("Текст", { onCommand(PostMenuCommand.COPY_TEXT) }, icon = UiIconKind.DOCUMENT)
                ActionMenuItem("Разметку", { onCommand(PostMenuCommand.COPY_MARKUP) }, icon = UiIconKind.CODE)
            }
            ActionMenuItem("Ссылку", { onCommand(PostMenuCommand.COPY_LINK) }, options.hasLink, icon = UiIconKind.LINK)
        }

        PostMenuPage.HIDE -> {
            ActionMenuItem("Этот пост", { onCommand(PostMenuCommand.HIDE) }, icon = UiIconKind.HIDE)
            ActionMenuItem("Дерево ответов", { onCommand(PostMenuCommand.HIDE_TREE) }, icon = UiIconKind.HIDE)
            ActionMenuItem("Посты с тем же именем", { onCommand(PostMenuCommand.HIDE_NAME) }, icon = UiIconKind.PERSON)
            ActionMenuItem(
                "Похожие посты",
                { onCommand(PostMenuCommand.HIDE_SIMILAR) },
                options.hasText,
                icon = UiIconKind.FILTER,
            )
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
    ActionMenuItem("Ответить", { onCommand(PostMenuCommand.REPLY) }, !options.closed, icon = UiIconKind.REPLY)
    ActionMenuItem(
        "Цитировать",
        { onCommand(PostMenuCommand.QUOTE) },
        !options.closed && options.hasText,
        icon = UiIconKind.QUOTE,
    )
    ActionMenuItem(
        "Копировать",
        { onPage(PostMenuPage.COPY) },
        navigation = MenuNavigation.FORWARD,
        icon = UiIconKind.COPY,
    )
    ActionMenuItem("Пожаловаться", { onCommand(PostMenuCommand.REPORT) }, options.canReport, icon = UiIconKind.WARNING)
    if (options.storageActions) {
        TextButton(
            onClick = { onCommand(PostMenuCommand.OWN) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            UiMenuIcon(UiIconKind.PERSON)
            Spacer(Modifier.width(12.dp))
            Text("Моё сообщение", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Checkbox(options.own, onCheckedChange = null)
        }
    }
    if (canAddRule) {
        ActionMenuItem(
            "Добавить правило",
            { onCommand(PostMenuCommand.ADD_FILTER) },
            icon = UiIconKind.FILTER,
        )
    }
    if (options.storageActions && !options.hidden) {
        ActionMenuItem(
            "Скрыть",
            { onPage(PostMenuPage.HIDE) },
            navigation = MenuNavigation.FORWARD,
            icon = UiIconKind.HIDE,
        )
    }
}
