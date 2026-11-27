package com.solthariel.pawchan.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import com.solthariel.pawchan.R.string as PlatformStrings

/** No application toast/snackbar; system clipboard feedback is owned by Android. */
internal fun Context.copyPublicLink(link: String) {
    getSystemService<ClipboardManager>()?.setPrimaryClip(
        ClipData.newPlainText(getString(PlatformStrings.platform_link), link),
    )
}
