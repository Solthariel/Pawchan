package com.solthariel.pawchan.nativebuild

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class ArchiveCacheTest {
    @get:Rule val temp = TemporaryFolder()
    private val bytes = "verified archive".toByteArray()

    @Test fun verifiedDownloadIsReusableOffline() {
        var calls = 0
        val cache =
            ArchiveCache(
                temp.root.toPath(),
                ArchiveTransport { _, output ->
                    calls++
                    Files.write(output, bytes)
                },
            )
        val first = cache.obtain(Fixtures.dependency(bytes), false)
        assertEquals(first, cache.obtain(Fixtures.dependency(bytes), true))
        assertEquals(1, calls)
        assertArrayEquals(bytes, Files.readAllBytes(first))
    }

    @Test fun offlineMissNeverDownloads() {
        val cache = ArchiveCache(temp.root.toPath(), ArchiveTransport { _, _ -> fail("Unexpected network") })
        assertThrows(SourceUnavailable::class.java) { cache.obtain(Fixtures.dependency(bytes), true) }
    }

    @Test fun corruptedOfflineArchiveIsRejected() {
        val spec = Fixtures.dependency(bytes)
        Fixtures.write(temp.root.toPath().resolve("${spec.sha256}.archive"), "corruption")
        val cache = ArchiveCache(temp.root.toPath(), ArchiveTransport { _, _ -> fail("Unexpected network") })
        assertThrows(IntegrityFailure::class.java) { cache.obtain(spec, true) }
    }

    @Test fun corruptedOnlineArchiveIsReplacedOnlyWithVerifiedBytes() {
        val spec = Fixtures.dependency(bytes)
        Fixtures.write(temp.root.toPath().resolve("${spec.sha256}.archive"), "corruption")
        val cache = ArchiveCache(temp.root.toPath(), ArchiveTransport { _, output -> Files.write(output, bytes) })
        assertArrayEquals(bytes, Files.readAllBytes(cache.obtain(spec, false)))
    }

    @Test fun hashMismatchDoesNotPublishOrLeaveTemporaryFiles() {
        val cache = ArchiveCache(temp.root.toPath(), ArchiveTransport { _, output -> Fixtures.write(output, "wrong content") })
        assertThrows(IntegrityFailure::class.java) { cache.obtain(Fixtures.dependency(bytes), false) }
        assertTrue(temp.root.listFiles()!!.isEmpty())
    }

    @Test fun networkFailureCleansPartialDownload() {
        val cache =
            ArchiveCache(
                temp.root.toPath(),
                ArchiveTransport {
                    _,
                    output,
                    ->
                    Files.write(output, byteArrayOf(1))
                    throw SourceUnavailable("offline")
                },
            )
        assertThrows(SourceUnavailable::class.java) { cache.obtain(Fixtures.dependency(bytes), false) }
        assertTrue(temp.root.listFiles()!!.isEmpty())
    }
}
