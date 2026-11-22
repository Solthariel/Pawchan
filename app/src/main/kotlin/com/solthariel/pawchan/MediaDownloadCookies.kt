package com.solthariel.pawchan

import android.webkit.CookieManager
import com.solthariel.pawchan.infrastructure.AndroidCookieJar

internal fun mediaDownloadCookieJar(
    mirror: String,
    manager: CookieManager = CookieManager.getInstance(),
): AndroidCookieJar = AndroidCookieJar(manager).apply { selectOrigin(mirror) }
