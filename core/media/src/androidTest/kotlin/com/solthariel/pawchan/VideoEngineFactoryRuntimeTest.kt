package com.solthariel.pawchan

import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.video.MpvVideoEngine
import com.solthariel.pawchan.media.video.videoEngine
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoEngineFactoryRuntimeTest {
    @Test
    fun defaultEngineIsMpv() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = videoEngine(context, OkHttpClient(), context.cacheDir)
        try {
            assertTrue("The default video engine must be mpv", engine is MpvVideoEngine)
        } finally {
            engine.close()
        }
    }
}
