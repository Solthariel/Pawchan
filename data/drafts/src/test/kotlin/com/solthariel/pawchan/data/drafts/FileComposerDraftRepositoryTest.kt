package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.drafts.ComposerDraftPayload
import com.solthariel.pawchan.domain.drafts.DraftImageReencoding
import com.solthariel.pawchan.domain.drafts.DraftMedia
import com.solthariel.pawchan.domain.drafts.DraftText
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class FileComposerDraftRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("biz")))

    private fun key(number: Long? = null) = ComposerDraftKey(board, number?.let { requireNotNull(ThreadId.parse(it)) })

    private fun payload(value: Int = 1) =
        ComposerDraftPayload(
            DraftText("subject", "comment", "email"),
            PostingOptions("name", sage = true, threadAuthor = true, icon = 7),
            listOf(
                DraftMedia(
                    ReplyAttachment("original.png", "image/png", ByteArray(8) { value.toByte() }),
                    MediaDraftOptions(
                        true,
                        true,
                        customName = "renamed",
                        reencoding = DraftImageReencoding(),
                        spoiler = true,
                        previousReencoding = DraftImageReencoding(quality = 87, scalePercent = 60),
                    ),
                    "2×4",
                    preview = byteArrayOf(3, 4),
                ),
            ),
        )

    @Test fun restoresAcrossRepositoryInstancesIncludingAllSettingsAndOwnedFiles() =
        runTest {
            val directory = temporary.newFolder()
            FileComposerDraftRepository(directory).save(key(), payload())
            val restored = FileComposerDraftRepository(directory).load(key())
            assertEquals(payload().text, restored?.text)
            assertEquals(payload().options, restored?.options)
            val file = requireNotNull(restored).media.single()
            assertEquals(payload().media.single().options, file.options)
            assertArrayEquals(
                payload()
                    .media
                    .single()
                    .source.content,
                file.source.content,
            )
            assertArrayEquals(byteArrayOf(3, 4), file.preview)
            assertTrue(requireNotNull(file.localUri).startsWith("file:"))
        }

    @Test fun latestFiveDraftsUseAccessOrderAndNeverCrossNewThreadReplyKeys() =
        runTest {
            val repository = FileComposerDraftRepository(temporary.newFolder())
            repository.save(key(), ComposerDraftPayload(DraftText(comment = "new")))
            for (number in 1L..4L) repository.save(key(number), ComposerDraftPayload(DraftText(comment = "$number")))
            repository.load(key())
            repository.save(key(5), ComposerDraftPayload(DraftText(comment = "five")))
            assertNull(repository.load(key(1)))
            assertEquals("new", repository.load(key())?.text?.comment)
            assertEquals("five", repository.load(key(5))?.text?.comment)
        }

    @Test fun sharedMediaIsNotDeletedUntilItsLastDraftIsRemoved() =
        runTest {
            val directory = temporary.newFolder()
            val repository = FileComposerDraftRepository(directory)
            repository.save(key(1), payload())
            repository.save(key(2), payload())
            assertEquals(2, File(directory, "media").listFiles()?.size)
            repository.remove(key(1))
            assertNotNull(repository.load(key(2)))
            assertEquals(2, File(directory, "media").listFiles()?.size)
            repository.remove(key(2))
            assertEquals(0, File(directory, "media").listFiles()?.size)
        }

    @Test fun byteBudgetEvictsOldDraftAndRemovesOnlyUnreferencedBlobs() =
        runTest {
            val directory = temporary.newFolder()
            val repository = FileComposerDraftRepository(directory, cacheLimitBytes = 18)
            repository.save(key(1), payload(1))
            repository.save(key(2), payload(2))
            repository.save(key(3), payload(3))
            assertNull(repository.load(key(1)))
            assertNotNull(repository.load(key(2)))
            assertNotNull(repository.load(key(3)))
            assertTrue(File(directory, "media").listFiles().orEmpty().sumOf { it.length() } <= 18)
        }

    @Test fun emptyDraftClearsFilesAndDoesNotBecomeAStoredBlankEntry() =
        runTest {
            val directory = temporary.newFolder()
            val repository = FileComposerDraftRepository(directory)
            repository.save(key(), payload())
            repository.save(key(), ComposerDraftPayload())
            assertNull(repository.load(key()))
            assertEquals(0, File(directory, "media").listFiles()?.size)
        }

    @Test fun orphanedAndInterruptedTemporaryFilesAreCleanedOnLoad() =
        runTest {
            val directory = temporary.newFolder()
            val repository = FileComposerDraftRepository(directory)
            repository.save(key(), payload())
            val orphan = File(directory, "media/orphan.tmp").apply { writeText("orphan") }
            assertNotNull(repository.load(key()))
            assertFalse(orphan.exists())
        }

    @Test fun damagedBlobFailsExplicitlyWithoutDeletingTheStoredIndex() =
        runTest {
            val directory = temporary.newFolder()
            val repository = FileComposerDraftRepository(directory)
            repository.save(key(), payload())
            val index = File(directory, "index.json").readText()
            File(directory, "media")
                .listFiles()
                .orEmpty()
                .first { it.length() == 8L }
                .writeBytes(ByteArray(8) { 99 })
            val failure = runCatching { repository.load(key()) }.exceptionOrNull()
            assertTrue(failure is IOException)
            assertEquals(index, File(directory, "index.json").readText())
        }

    @Test fun invalidIndexCannotEscapeCacheOrSilentlyDestroyExistingFiles() =
        runTest {
            val directory = temporary.newFolder()
            val repository = FileComposerDraftRepository(directory)
            repository.save(key(), payload())
            val file = File(directory, "index.json")
            val malicious = file.readText().replace(Regex("\"hash\":\"[a-f0-9]+\""), "\"hash\":\"../../outside\"")
            file.writeText(malicious)
            assertTrue(runCatching { repository.load(key()) }.exceptionOrNull() is IOException)
            assertEquals(malicious, file.readText())
            assertEquals(2, File(directory, "media").listFiles()?.size)
        }

    @Test fun concurrentRepositoryInstancesPreserveBothDrafts() =
        runTest {
            val directory = temporary.newFolder()
            val first = FileComposerDraftRepository(directory)
            val second = FileComposerDraftRepository(directory)
            val a = async { first.save(key(1), payload(1)) }
            val b = async { second.save(key(2), payload(2)) }
            a.await()
            b.await()
            assertNotNull(first.load(key(1)))
            assertNotNull(second.load(key(2)))
        }
}
