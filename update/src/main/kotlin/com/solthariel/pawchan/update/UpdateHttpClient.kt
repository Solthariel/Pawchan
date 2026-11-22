package com.solthariel.pawchan.update

import okhttp3.OkHttpClient

/** An isolated, anonymous client that owns its HTTP resources. */
internal fun updateHttpClient(configure: (OkHttpClient.Builder.() -> Unit)? = null): OkHttpClient =
    OkHttpClient
        .Builder()
        .apply {
            configure?.invoke(this)
        }.followSslRedirects(false)
        .build()
