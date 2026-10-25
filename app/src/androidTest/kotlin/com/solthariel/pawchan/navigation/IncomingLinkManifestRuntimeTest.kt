package com.solthariel.pawchan.navigation

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingLinkManifestRuntimeTest {
    @Test fun installedManifestAcceptsEverySupportedWebAlias() {
        listOf("2ch.hk", "2ch.su", "2ch.life", "2ch.org").forEach { host ->
            listOf("http", "https").forEach { scheme ->
                val url = "$scheme://$host/b/res/123.html#456"
                assertTrue(url, resolvesToMainActivity(url))
            }
        }
    }

    @Test fun installedManifestDoesNotClaimOtherOrigins() {
        listOf(
            "https://2ch.hk.evil.example/b/res/123.html",
            "https://evil.example/2ch.hk/b/res/123.html",
            "https://2ch.pm/b/res/123.html",
            "ftp://2ch.hk/b/res/123.html",
        ).forEach { assertFalse(it, resolvesToMainActivity(it)) }
    }

    private fun resolvesToMainActivity(url: String): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(context.packageName)
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .any { it.activityInfo.name == "com.solthariel.pawchan.MainActivity" }
    }
}
