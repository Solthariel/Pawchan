package com.solthariel.pawchan

import android.content.ComponentName
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DebugPackageIsolationRuntimeTest {
    @Test fun instrumentationAndApplicationHaveSeparateDebugIdentities() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        assertEquals("com.solthariel.pawchan.debug", app.packageName)
        assertEquals("com.solthariel.pawchan.debug.test", instrumentation.context.packageName)
        assertNotEquals("com.solthariel.pawchan", app.packageName)
        assertEquals("Pawchan Debug", app.applicationInfo.loadLabel(app.packageManager).toString())
    }

    @Test fun fileProviderUsesTheDebugAuthority() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val provider =
            app.packageManager.getProviderInfo(
                ComponentName(app, MediaFileProvider::class.java),
                0,
            )
        assertEquals("com.solthariel.pawchan.debug.media", provider.authority)
        assertEquals(app.packageName, provider.packageName)
    }
}
