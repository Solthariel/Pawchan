package com.solthariel.pawchan.arhivach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.FocusWhenOpened
import com.solthariel.pawchan.core.ui.ModalDropdownMenu
import com.solthariel.pawchan.core.ui.ThreadToolbarActionLayout
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiRefreshButton
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.core.ui.UiToolbarSearchField
import com.solthariel.pawchan.core.ui.UiToolbarSearchTransition
import com.solthariel.pawchan.thread.ThreadMenuItem
import com.solthariel.pawchan.ui.copyPublicLink
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ArchiveThreadToolbar(
    state: ArchiveBrowseState,
    actions: ArchiveToolbarActions,
    search: ArchiveWordSearch,
    galleryEnabled: Boolean,
) {
    val res = LocalResources.current

    var menuOpen by rememberSaveable(state.selectedThread) { mutableStateOf(false) }
    val context = LocalContext.current
    Box(Modifier.fillMaxWidth().heightIn(min = APP_TOP_BAR_HEIGHT), contentAlignment = Alignment.Center) {
        UiToolbarSearchTransition(searchVisible = search.visible, label = "thread-search") { searchOpen ->
            if (searchOpen) {
                ArchiveThreadSearchBar(search)
            } else {
                ThreadToolbarActionLayout(
                    leading = {
                        UiToolbarButton(
                            UiIconKind.BACK,
                            res.getString(L.ui_back),
                            onClick = actions.onThreadBack ?: actions.model::backToList,
                        )
                        UiToolbarButton(
                            UiIconKind.MEDIA_MULTIPLE,
                            res.getString(L.ui_gallery),
                            enabled = galleryEnabled,
                            onClick = actions.onGalleryClick,
                        )
                    },
                    trailing = {
                        UiToolbarButton(
                            UiIconKind.SEARCH,
                            res.getString(L.ui_search),
                            enabled =
                                state.thread != null,
                        ) {
                            search.setVisible(true)
                        }
                        UiRefreshButton(
                            description = res.getString(L.ui_refresh),
                            isRefreshing = state.load.loading,
                            enabled = !state.load.loading,
                            onClick = actions.model::refresh,
                        )
                        Box {
                            UiToolbarButton(
                                UiIconKind.MORE,
                                res.getString(L.ui_thread_menu),
                                enabled =
                                    state.thread != null,
                            ) {
                                menuOpen = true
                            }
                            ModalDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                ThreadMenuItem(res.getString(L.ui_copy_link), icon = UiIconKind.LINK) {
                                    menuOpen = false
                                    archiveThreadLink(state.selectedThread)?.let { context.copyPublicLink(it) }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ArchiveThreadSearchBar(search: ArchiveWordSearch) {
    val res = LocalResources.current

    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val close = {
        focusManager.clearFocus()
        keyboard?.hide()
        search.setVisible(false)
        search.setQuery("")
    }
    BackHandler(onBack = close)
    FocusWhenOpened(focus)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = APP_TOP_BAR_HEIGHT,
            ).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiToolbarButton(UiIconKind.BACK, res.getString(L.ui_close_search), onClick = close)
        UiToolbarSearchField(
            query = search.query,
            onQueryChange = search.setQuery,
            focusRequester = focus,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            placeholder = res.getString(L.ui_search_in_thread),
        )
    }
}
