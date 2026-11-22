package com.solthariel.pawchan

import android.webkit.CookieManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaDownloadCookiesRuntimeTest {
    @Test fun workerUsesSelectedMirrorCookiesWithoutSendingThemElsewhere() {
        val manager = CookieManager.getInstance()
        val origin = "https://pawchan-cookie-test.invalid/"
        manager.setAcceptCookie(true)
        manager.setCookie(origin, "paw_test=fixture; Path=/; Secure")
        manager.flush()
        try {
            val jar = mediaDownloadCookieJar("pawchan-cookie-test.invalid", manager)
            val cookies = jar.loadForRequest("${origin}b/src/picture.jpg".toHttpUrl())
            assertEquals("fixture", cookies.single { it.name == "paw_test" }.value)
            assertTrue(jar.loadForRequest("https://other.invalid/file".toHttpUrl()).isEmpty())
        } finally {
            manager.setCookie(origin, "paw_test=; Max-Age=0; Path=/; Secure")
            manager.flush()
        }
    }
}
