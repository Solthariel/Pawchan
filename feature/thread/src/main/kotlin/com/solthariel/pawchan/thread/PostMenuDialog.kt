package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppDialog

@Composable
internal fun PostMenuDialog(
    options: PostMenuOptions,
    onCommand: (PostMenuCommand) -> Unit,
    onClose: () -> Unit,
    canAddRule: Boolean = false,
) {
    var page by remember { mutableStateOf(PostMenuPage.MAIN) }
    val back = { if (page == PostMenuPage.MAIN) onClose() else page = PostMenuPage.MAIN }
    AppDialog(back) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
            if (page != PostMenuPage.MAIN) ActionMenuItem("‹ Назад", { page = PostMenuPage.MAIN })
            PostMenuContent(page, options, { page = it }, onCommand, canAddRule)
        }
    }
}
