package com.solthariel.pawchan.settings

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.infrastructure.CookieRules
import com.solthariel.pawchan.R.string as L
import com.solthariel.pawchan.core.ui.ModalDropdownMenu as DropdownMenu

internal data class CookieEntry(
    val name: String,
    /** null: куки сейчас нет, осталось только правило. */
    val value: String?,
    val blocked: Boolean,
    val deleteOnExit: Boolean,
)

/** Сами куки и имена с правилами (заблокированная кука не хранится, но разблокировать её можно). */
internal fun cookieEntries(
    cookies: List<Pair<String, String>>,
    rules: CookieRules,
): List<CookieEntry> {
    val values = cookies.toMap()
    return (values.keys + rules.blocked + rules.deleteOnExit)
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .map { name -> CookieEntry(name, values[name], name in rules.blocked, name in rules.deleteOnExit) }
}

/** Значение скрыто точками той же примерной длины, пока не нажат глаз. */
internal fun maskedCookieValue(value: String): String = "•".repeat(value.length.coerceIn(MIN_MASK, MAX_MASK))

internal fun Resources.cookieCaption(
    entry: CookieEntry,
    revealed: Boolean,
): String =
    listOfNotNull(
        entry.value?.let { if (revealed) it else maskedCookieValue(it) },
        getString(L.ui_cookie_blocked).takeIf { entry.blocked },
        getString(L.ui_cookie_deleted_on_exit).takeIf { entry.deleteOnExit },
    ).joinToString(" · ")

/** Нажатие открывает меню: блокировать, удалять при выходе, удалить. */
@Composable
internal fun CookieRow(
    entry: CookieEntry,
    controller: CookiesController,
) {
    val res = LocalResources.current
    var revealed by rememberSaveable(entry.name) { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    res.cookieCaption(entry, revealed),
                    maxLines = if (revealed) REVEALED_LINES else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = { if (entry.value != null) RevealToggle(entry.name, revealed) { revealed = it } },
            modifier = Modifier.fillMaxWidth().clickable { menu = true }.testTag("cookie-${entry.name}"),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            CookieMenuItems(entry, controller) { menu = false }
        }
    }
}

@Composable
private fun CookieMenuItems(
    entry: CookieEntry,
    controller: CookiesController,
    close: () -> Unit,
) {
    val res = LocalResources.current
    ActionMenuItem(res.getString(if (entry.blocked) L.ui_cookie_unblock else L.ui_cookie_block), {
        close()
        controller.block(entry.name, !entry.blocked)
    }, icon = UiIconKind.BANNED)
    ActionMenuItem(res.getString(L.ui_cookie_delete_on_exit), {
        close()
        controller.deleteOnExit(entry.name, !entry.deleteOnExit)
    }, icon = if (entry.deleteOnExit) UiIconKind.CHECK else null)
    ActionMenuItem(res.getString(L.ui_delete), {
        close()
        controller.delete(entry.name)
    }, enabled = entry.value != null, icon = UiIconKind.DELETE)
}

@Composable
private fun RevealToggle(
    name: String,
    revealed: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val description = LocalResources.current.getString(L.ui_cookie_show_value, name)
    IconToggleButton(
        checked = revealed,
        onCheckedChange = onChange,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        UiIcon(
            if (revealed) UiIconKind.HIDE else UiIconKind.SHOW,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Modifier.size(24.dp),
        )
    }
}

private const val MIN_MASK = 4
private const val MAX_MASK = 16
private const val REVEALED_LINES = 4
