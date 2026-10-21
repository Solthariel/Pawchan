package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerPreparedUpload
import com.solthariel.pawchan.composer.composerProgressObserver
import com.solthariel.pawchan.composer.composerUploadBudget
import com.solthariel.pawchan.composer.preparedSelectionAllowed
import com.solthariel.pawchan.composer.preparedUpload
import com.solthariel.pawchan.composer.remainingUploadMegabytes
import com.solthariel.pawchan.composer.uploadStatusText
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.PostingUploadProgress
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerUploadBudgetTest {
    private val rules = PostingRules(maxTotalFileBytes = 40)

    private fun file(bytes: Int) = ComposerAttachment(ReplyAttachment("clip.mp4", "video/mp4", ByteArray(bytes)))

    @Test fun exactBytesDetermineEligibilityAndDoNotDiscardOverBudgetFiles() {
        val files = listOf(file(20), file(21))
        assertEquals(-1L, composerUploadBudget(files, rules).remaining)
        assertEquals("Превышен общий размер вложений", composerUploadBudget(files, rules).status)
        assertFalse(preparedSelectionAllowed(files, rules))
        assertTrue(preparedSelectionAllowed(listOf(file(20), file(20)), rules))
        assertEquals(2, files.size)
        assertEquals("−<0,1", remainingUploadMegabytes(-1))
        assertEquals("0", remainingUploadMegabytes(0))
        assertEquals("−1", remainingUploadMegabytes(-1024L * 1024))
    }

    @Test fun counterUsesCachedProcessedBytesAndInvalidatesThemOnEdit() {
        val original = file(30)
        val options = MediaDraftOptions(uniqueHash = true)
        val upload = original.source.copy(content = ByteArray(10))
        val processed =
            original.copy(
                options = options,
                prepared = ComposerPreparedUpload(original.source, options, upload),
            )
        assertSame(upload, processed.preparedUpload)
        assertEquals(30L, composerUploadBudget(listOf(processed), rules).remaining)
        val edited = processed.copy(options = options.copy(removeMetadata = true))
        assertNull(edited.preparedUpload)
        assertEquals("Расчёт размера…", composerUploadBudget(listOf(edited), rules).status)
        assertFalse(preparedSelectionAllowed(listOf(edited), rules))
    }

    @Test fun canceledSubmissionCannotDeliverProgressIntoAnotherSession() {
        val job = Job()
        val updates = kotlinx.coroutines.flow.MutableStateFlow(0)
        val observer = composerProgressObserver(job, updates) { count, _ -> count + 1 }
        observer.onProgress(PostingUploadProgress(1, 10))
        job.cancel()
        observer.onProgress(PostingUploadProgress(2, 10))
        assertEquals(1, updates.value)
    }

    @Test fun uploadAndServerWaitAreDistinctStatuses() {
        assertEquals("Отправка…", uploadStatusText(null))
        assertEquals("Отправка 40%", uploadStatusText(PostingUploadProgress(4, 10)))
        assertEquals("Ожидание ответа…", uploadStatusText(PostingUploadProgress(10, 10, true)))
    }
}
