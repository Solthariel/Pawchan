package com.solthariel.pawchan.update

import okhttp3.Dns
import okhttp3.OkHttpClient

internal fun privateUpdateHttpClient(
    dns: Dns,
    repository: String?,
    access: UpdateTokenStore?,
): OkHttpClient =
    OkHttpClient
        .Builder()
        .dns(dns)
        .followSslRedirects(false)
        .apply {
            if (repository != null) {
                val store = checkNotNull(access) { "Private update store is missing" }
                val policy = GithubUpdateAuth(GithubUpdateUrls(repository), store::read)
                addInterceptor(policy)
                addNetworkInterceptor(policy)
            }
        }.build()
