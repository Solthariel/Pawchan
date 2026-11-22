package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AttachmentSaveStateTest {
    private val file =
        ComposerAttachment(
            ReplyAttachment("photo.png", "image/png", byteArrayOf(1)),
            options = MediaDraftOptions(image = DraftImageOptions(edit = DraftImageEdit(quarterTurns = 1))),
        )

    @Test
    fun savesOnlyPreparedBytesAndCoalescesRepeatedClicks() =
        runTest {
            val rendered = file.source.copy(content = byteArrayOf(2))
            var calls = 0
            var saved: ComposerAttachment? = null
            val state =
                AttachmentSaveState(backgroundScope, StandardTestDispatcher(testScheduler), {
                    calls++
                    rendered
                })
            state.save(file) { saved = it }
            state.save(file) { saved = it }
            assertTrue(state.busy)
            assertNull(saved)
            runCurrent()
            assertFalse(state.busy)
            assertEquals(1, calls)
            assertSame(rendered, saved?.preparedUpload)
            assertSame(file.source, saved?.source)
            assertNull(state.error)
        }

    @Test
    fun failedExportKeepsDraftOpenAndCanBeRetriedWithoutFallback() =
        runTest {
            var fail = true
            var saved: ComposerAttachment? = null
            val state =
                AttachmentSaveState(backgroundScope, StandardTestDispatcher(testScheduler), {
                    require(!fail) { "decode" }
                    file.source.copy(content = byteArrayOf(2))
                })
            state.save(file) { saved = it }
            runCurrent()
            assertNull(saved)
            assertNotNull(state.error)
            assertFalse(state.busy)
            assertNull(file.preparedUpload)
            fail = false
            state.save(file) { saved = it }
            runCurrent()
            assertNotNull(saved?.preparedUpload)
            assertNull(state.error)
        }

    @Test
    fun cancelledExportNeverPublishesSourceOrReportsSuccess() =
        runTest {
            var published = false
            val state =
                AttachmentSaveState(backgroundScope, StandardTestDispatcher(testScheduler), {
                    throw CancellationException("viewer closed")
                })
            state.save(file) { published = true }
            runCurrent()
            assertFalse(published)
            assertFalse(state.busy)
            assertNull(state.error)
        }

    @Test fun repeatedInvalidFilenameShowsNewNoticeWithoutPublishing() =
        runTest {
            val invalid = file.copy(options = file.options.copy(customName = "../bad"))
            val state = AttachmentSaveState(backgroundScope, StandardTestDispatcher(testScheduler))
            var published = false
            state.save(invalid) { published = true }
            val first = state.noticeVersion
            state.save(invalid) { published = true }
            assertTrue(state.noticeVersion > first)
            assertEquals("Недопустимое имя файла", state.error)
            assertEquals("../bad", invalid.options.customName)
            assertFalse(published)
            assertFalse(state.busy)
        }
}
