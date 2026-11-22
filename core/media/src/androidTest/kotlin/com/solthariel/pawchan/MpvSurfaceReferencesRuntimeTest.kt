package com.solthariel.pawchan

import android.graphics.SurfaceTexture
import android.view.Surface
import io.github.yuroyami.libmpvkt.jni.MpvNativeApi
import io.github.yuroyami.libmpvkt.jni.MpvSurfaceReferences
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises the actual Android JNI entry points and global-reference lifecycle. */
@OptIn(MpvNativeApi::class)
class MpvSurfaceReferencesRuntimeTest {
    @Test
    fun surfaceReferencesCanBeCreatedAndReleasedWithoutReleasingSurface() {
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        try {
            repeat(10) {
                val reference = MpvSurfaceReferences.create(surface)
                assertNotEquals(0L, reference)
                MpvSurfaceReferences.release(reference)
                assertTrue(surface.isValid)
            }
            MpvSurfaceReferences.release(0)
        } finally {
            surface.release()
            texture.release()
        }
    }
}
