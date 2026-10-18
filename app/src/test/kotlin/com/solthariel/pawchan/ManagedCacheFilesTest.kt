package com.solthariel.pawchan

import com.solthariel.pawchan.infrastructure.clearClosedSessionCache
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ManagedCacheFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun clearsOnlyOwnedCacheAndKeepsOtherFiles() {
        val root = temporary.newFolder("cache")
        val thumbnails = File(root, "thumbnail_cache").apply { mkdirs() }
        File(thumbnails, "preview").writeText("temporary")
        File(root, "neko-ffmpeg-123.media").writeText("temporary")
        val unrelated = File(root, "keep.txt").apply { writeText("keep") }
        val drafts = File(temporary.root, "composer-drafts").apply { mkdirs() }
        File(drafts, "draft").writeText("keep")
        assertTrue(clearClosedSessionCache(root))
        assertFalse(thumbnails.exists())
        assertTrue(File(root, "neko-ffmpeg-123.media").exists())
        assertTrue(unrelated.exists())
        assertTrue(File(drafts, "draft").exists())
    }

    @Test
    fun absentCacheIsAlreadyClean() {
        assertTrue(clearClosedSessionCache(File(temporary.root, "missing")))
    }
}
