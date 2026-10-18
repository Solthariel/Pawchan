package com.solthariel.pawchan.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.domain.model.ThreadAddress

internal data class CatalogThreadInteractions(
    val hidden: Set<ThreadAddress>,
    val onClick: (ThreadAddress) -> Unit,
    val onLongClick: (CatalogThreadUi) -> Unit,
)

/** Reference ThreadsPage: short tap opens (or restores); long press favorites, copies, shares, or hides. */
@Composable
internal fun rememberCatalogThreadMenu(
    onOpen: (ThreadAddress) -> Unit,
    host: CatalogThreadMenuHost?,
    onAddRule: ((CatalogThreadUi) -> Unit)? = null,
): CatalogThreadInteractions {
    val hidden =
        host
            ?.hiddenThreads
            ?.collectAsStateWithLifecycle()
            ?.value
            .orEmpty()
    var selected by remember { mutableStateOf<CatalogThreadUi?>(null) }
    selected?.let { thread ->
        CatalogThreadMenu(thread, host, onAddRule) { selected = null }
    }
    return CatalogThreadInteractions(hidden, onClick = { address ->
        if (address in hidden) {
            host?.setHidden?.invoke(address, false)
        } else {
            onOpen(address)
        }
    }, onLongClick = { selected = it })
}

@Composable
private fun CatalogThreadMenu(
    thread: CatalogThreadUi,
    host: CatalogThreadMenuHost?,
    onAddRule: ((CatalogThreadUi) -> Unit)?,
    onClose: () -> Unit,
) {
    val link = host?.threadLink?.invoke(thread.address)
    var favorite by remember(thread.address) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(thread.address, host) { favorite = host?.isFavorite?.invoke(thread.address) }
    CatalogThreadMenuDialog(
        CatalogThreadMenuOptions(
            hidden = thread.hidden,
            canHide = host != null,
            canLink = link != null,
            favorite = favorite,
            canAddRule = onAddRule != null,
        ),
        onClose = onClose,
        callbacks =
            CatalogThreadMenuCallbacks(
                addRule = {
                    onClose()
                    onAddRule?.invoke(thread)
                },
                copy = {
                    onClose()
                    link?.let { host?.copyLink?.invoke(it) }
                },
                share = {
                    onClose()
                    link?.let { host?.shareLink?.invoke(it) }
                },
                hide = {
                    onClose()
                    host?.setHidden?.invoke(thread.address, true)
                },
                favorite = {
                    onClose()
                    favorite?.let { host?.setFavorite?.invoke(thread.address, thread.subject, !it) }
                },
            ),
    )
}

@Composable
private fun CatalogThreadMenuDialog(
    options: CatalogThreadMenuOptions,
    onClose: () -> Unit,
    callbacks: CatalogThreadMenuCallbacks,
) {
    AppMenuDialog(onClose) {
        if (options.canHide) {
            ActionMenuItem(
                if (options.favorite == true) "Убрать тред из избранного" else "Добавить тред в избранное",
                callbacks.favorite,
                options.favorite != null,
            )
        }
        ActionMenuItem("Копировать ссылку", callbacks.copy, options.canLink)
        ActionMenuItem("Поделиться ссылкой", callbacks.share, options.canLink)
        if (options.canAddRule) ActionMenuItem("Добавить правило", callbacks.addRule)
        if (!options.hidden && options.canHide) ActionMenuItem("Скрыть", callbacks.hide)
    }
}

private data class CatalogThreadMenuOptions(
    val hidden: Boolean,
    val canHide: Boolean,
    val canLink: Boolean,
    val favorite: Boolean?,
    val canAddRule: Boolean,
)

private data class CatalogThreadMenuCallbacks(
    val copy: () -> Unit,
    val share: () -> Unit,
    val hide: () -> Unit,
    val favorite: () -> Unit,
    val addRule: () -> Unit,
)
