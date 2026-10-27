package com.solthariel.pawchan.settings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.solthariel.pawchan.R

internal val LocalFilterCopyNotice = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
internal fun FilterRegexCopyButton(sourceText: String) {
    val context = LocalContext.current
    val notice = LocalFilterCopyNotice.current
    IconButton(onClick = {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Условие автоскрытия", filterRegexPrompt(sourceText)))
            notice()
        }
    }) {
        Icon(painterResource(R.drawable.ic_copy), contentDescription = "Копировать")
    }
}
