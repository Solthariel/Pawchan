package com.solthariel.pawchan.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

/** No application toast/snackbar; system clipboard feedback is owned by Android. */
internal fun Context.copyPublicLink(link: String) {
    getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText("Ссылка", link))
}
