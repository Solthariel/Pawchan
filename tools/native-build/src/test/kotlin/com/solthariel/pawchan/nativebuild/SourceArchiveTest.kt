package com.solthariel.pawchan.nativebuild

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class SourceArchiveTest {
    @get:Rule val temp = TemporaryFolder()

    private fun extract(bytes: ByteArray) {
        val archive = temp.root.toPath().resolve("source.tar.gz")
        Files.write(archive, bytes)
        SourceArchive.extract(archive, temp.root.toPath().resolve("unpacked"))
    }

    @Test fun regularFilesAreExtractedWithoutWrapperAndKeepExecutableMode() {
        extract(Fixtures.archive("upstream/configure", "upstream/nested/file"))
        assertEquals("AAAA", Files.readString(temp.root.toPath().resolve("unpacked/configure")))
        assertTrue(Files.isExecutable(temp.root.toPath().resolve("unpacked/configure")))
    }

    @Test fun parentTraversalIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { extract(Fixtures.archive("upstream/../../escape")) }
        assertFalse(Files.exists(temp.root.toPath().resolve("escape")))
    }

    @Test fun absolutePathIsRejected() {
        val entry = TarArchiveEntry("/tmp/pawchan-archive-escape", true).apply { size = 4 }
        assertThrows(IllegalArgumentException::class.java) { extract(Fixtures.tar(listOf(entry))) }
    }

    @Test fun symlinkIsRejected() {
        val entry = TarArchiveEntry("upstream/link", TarConstants.LF_SYMLINK).apply { linkName = "../../outside" }
        assertThrows(IllegalArgumentException::class.java) { extract(Fixtures.tar(listOf(entry))) }
    }

    @Test fun hardLinkIsRejected() {
        val entry = TarArchiveEntry("upstream/link", TarConstants.LF_LINK).apply { linkName = "../../outside" }
        assertThrows(IllegalArgumentException::class.java) { extract(Fixtures.tar(listOf(entry))) }
    }

    @Test fun multipleRootsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { extract(Fixtures.archive("first/a", "second/b")) }
    }

    @Test fun duplicateFilesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { extract(Fixtures.archive("upstream/a", "upstream/a")) }
    }

    @Test fun emptyArchiveIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { extract(Fixtures.archive()) }
    }
}
