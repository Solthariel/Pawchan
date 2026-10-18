package com.solthariel.pawchan.archive

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Итоги операций с копиями («Копия сохранена», «Копия обновлена»…) всплывают на любом экране. */
@Composable
internal fun BindArchiveNotices(controller: ArchiveController) {
    val context = LocalContext.current
    val notice = controller.notice.collectAsStateWithLifecycle().value
    LaunchedEffect(notice) {
        if (notice != null) {
            Toast.makeText(context, notice, Toast.LENGTH_SHORT).show()
            controller.consumeNotice()
        }
    }
}
