package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.solthariel.pawchan.feature.thread.R.string as L

/**
 * Пункты Архивача: «Сохранить в Архивач» (капча), после отправки — «Восстановить из Архивача» и
 * «Открыть на Архиваче». Сетевая проверка не блокирует повторную попытку;
 * «Открыть» отдаёт ссылку браузеру и доступен всегда.
 */
@Composable
internal fun ThreadArhivachItems(
    address: ThreadAddress,
    tools: ThreadToolsState,
    history: ThreadHistoryActions?,
    /** Область меню-владельца: пункты исчезают при закрытии меню, загрузка должна продолжиться. */
    scope: CoroutineScope,
) {
    val res = LocalResources.current

    val external = LocalThreadArchive.current?.external ?: return
    val state = external.state(address)
    val link = state.link
    if (link == null) {
        ThreadMenuItem(
            res.getString(L.ui_save_to),
            icon = UiIconKind.ARCHIVE,
        ) { tools.openDialog(ThreadDialog.ARHIVACH) }
    } else {
        ThreadMenuItem(res.getString(L.ui_restore_from_archiver), enabled = history != null) {
            tools.closeMenu()
            if (history != null) scope.restoreFromArhivach(external, address, link, tools, history)
        }
        ThreadMenuItem(res.getString(L.ui_open_on_arhivach)) {
            tools.closeMenu()
            external.open(link)
        }
    }
}

private fun CoroutineScope.restoreFromArhivach(
    external: ThreadExternalArchive,
    address: ThreadAddress,
    link: String,
    tools: ThreadToolsState,
    history: ThreadHistoryActions,
) {
    tools.notice = "Загрузка копии с Архивача…"
    launch {
        when (val result = external.load(address, link)) {
            is DomainOutcome.Success -> {
                history.restore(result.value) { count ->
                    tools.notice =
                        if (count > 0) "Восстановлено удалённых постов: $count" else "Новых удалённых постов нет"
                }
            }

            is DomainOutcome.Failure -> {
                tools.notice = "Не удалось загрузить копию с Архивача"
            }
        }
    }
}
