package com.solthariel.pawchan.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast

/** Копирует текст; до Android 13 сам показывает «Скопировано», дальше это делает система. */
fun copyTextToClipboard(
    context: Context,
    label: String,
    text: String,
) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, context.getString(R.string.core_ui_copied), Toast.LENGTH_SHORT).show()
    }
}
