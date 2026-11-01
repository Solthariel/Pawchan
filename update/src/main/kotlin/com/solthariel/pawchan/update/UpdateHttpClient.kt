package com.solthariel.pawchan.update

import okhttp3.Dns
import okhttp3.OkHttpClient

/** An isolated, anonymous client; borrows DNS but owns its HTTP resources. */
internal fun updateHttpClient(dns: Dns): OkHttpClient =
    OkHttpClient
        .Builder()
        .dns(dns)
        .followSslRedirects(false)
        .build()
