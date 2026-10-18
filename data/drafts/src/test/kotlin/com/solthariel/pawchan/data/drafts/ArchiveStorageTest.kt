package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.archive.ArchiveMediaState
import com.solthariel.pawchan.domain.archive.ArchivePhase
import com.solthariel.pawchan.domain.archive.ArchiveState
import com.solthariel.pawchan.domain.archive.ArchivedMedia
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import com.solthariel.pawchan.domain.workspace.ThreadMarks
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream

class ArchiveStorageTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(100)),
        )

    private fun snapshot() =
        ThreadSnapshot(
            address,
            "<title>",
            ThreadFlags(false, false, false),
            listOf(
                BoardPost(
                    PostAddress(address, requireNotNull(PostId.parse(100))),
                    PostContent(
                        PostDocument(listOf(PostTextRun("<script>alert(1)</script>"))),
                        PostMetadata(null, null, false),
                    ),
                    emptyList(),
                ),
            ),
        )

    @Test fun archiveMetadataAndPositionRoundTripAndOldCopiesStillLoad() {
        val copy =
            ThreadCopy(
                snapshot(),
                1,
                ArchiveState(
                    true,
                    "https://arhivach.vc/thread/55/",
                    2,
                    ArchivePhase.WATCHING,
                    media = ArchiveMediaState(listOf(ArchivedMedia("https://2ch.su/x.png", "a".repeat(64), 3))),
                ),
                OpenThread(address, "title", 100, 42),
            )
        val links = mapOf(address to "https://arhivach.vc/thread/77/")
        val state = WorkspaceState(copies = listOf(copy), marks = ThreadMarks(arhivach = links))
        val decoded = decodeWorkspace(encodeWorkspace(state))
        val result = decoded.copies.single()
        assertEquals(links, decoded.marks.arhivach)
        assertEquals(copy.archive, result.archive)
        assertEquals(42, result.position?.offset)
        val old = copy.encodeCopy().filterKeys { it != "archive" && it != "position" }
        assertEquals(
            ArchiveState(),
            kotlinx.serialization.json
                .JsonObject(old)
                .decodeCopy()
                .archive,
        )
    }

    @Test fun pathTraversalIsRejectedAndZipHasPortableStructuredSnapshot() =
        runTest {
            val directory = Files.createTempDirectory("archive-test").toFile()
            try {
                val store = ArchiveMediaStore(directory, OkHttpClient())
                assertNull(store.resolve(ArchivedMedia("url", "../secret", 3)))
                val output = ByteArrayOutputStream()
                ArchiveZipWriter(store).write(ThreadCopy(snapshot(), 1), output)
                val entries = linkedMapOf<String, String>()
                ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                    }
                }
                assertEquals(setOf("snapshot.json", "index.html"), entries.keys)
                assertTrue(requireNotNull(entries["index.html"]).contains("&lt;script&gt;"))
                assertFalse(requireNotNull(entries["index.html"]).contains("<script>"))
            } finally {
                directory.deleteRecursively()
            }
        }

    @Test fun streamLimitProtectsAgainstUnknownContentLength() =
        runTest {
            try {
                archiveCopyBytes(ByteArrayInputStream(ByteArray(20)), ByteArrayOutputStream(), 10)
                fail("Limit expected")
            } catch (
                _: java.io.IOException,
            ) {
                Unit
            }
        }

    @Test fun pruningDoesNotDeleteFilesSharedWithOtherCopies() =
        runTest {
            val directory = Files.createTempDirectory("archive-prune").toFile()
            try {
                val keep = "a".repeat(64)
                val remove = "b".repeat(64)
                File(directory, keep).writeText("keep")
                File(directory, remove).writeText("remove")
                ArchiveMediaStore(directory, OkHttpClient()).prune(setOf(keep))
                assertTrue(File(directory, keep).isFile)
                assertFalse(File(directory, remove).exists())
            } finally {
                directory.deleteRecursively()
            }
        }
}
