package com.solthariel.pawchan.media.video

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertSame
import org.junit.Test

class MediaLocaleHostRuntimeTest {
    @Test fun pipPassesItsOwnBaseToTheApplicationHost() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val result = ContextWrapper(context)
        var received: Context? = null
        val app =
            object : ContextWrapper(context), MediaLocaleHost {
                override fun localizedMediaContext(base: Context): Context {
                    received = base
                    return result
                }
            }
        val base = withAppContext(context, app)
        assertSame(result, base.localizedForMedia())
        assertSame(base, received)
    }

    @Test fun missingHostKeepsTheOriginalContext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val base = withAppContext(context, ContextWrapper(context))
        assertSame(base, base.localizedForMedia())
    }

    private fun withAppContext(
        base: Context,
        app: Context,
    ): Context =
        object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = app
        }
}
