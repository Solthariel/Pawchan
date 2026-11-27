package com.solthariel.pawchan.media.video

import android.content.Context

/** The installing app owns language selection and packaging, not the reusable media library. */
fun interface MediaLocaleHost {
    fun localizedMediaContext(base: Context): Context
}

internal fun Context.localizedForMedia(): Context {
    val host = applicationContext as? MediaLocaleHost
    return host?.localizedMediaContext(this) ?: this
}
