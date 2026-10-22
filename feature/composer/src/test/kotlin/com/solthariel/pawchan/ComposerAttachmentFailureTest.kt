package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ATTACHMENT_PROCESSING_ERROR
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerUploadPreparation
import com.solthariel.pawchan.composer.preparedUpload
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComposerAttachmentFailureTest {
    @Test fun processingFailureRetainsOriginalAndOffersRecoveryInsteadOfRawRequireMessage() =
        runTest {
            val source = ReplyAttachment("image.png", "image/png", byteArrayOf(1))
            val file = ComposerAttachment(source, options = MediaDraftOptions(uniqueHash = true))
            val results = mutableListOf<List<ComposerAttachment>>()
            val owner =
                ComposerUploadPreparation(
                    backgroundScope,
                    StandardTestDispatcher(testScheduler),
                    prepare = {
                        require(false)
                        source
                    },
                    publish = results::add,
                )
            owner.update(listOf(file))
            runCurrent()
            val failed = results.single().single()
            assertSame(source, failed.source)
            assertEquals(ATTACHMENT_PROCESSING_ERROR, failed.prepared?.failure)
            assertNull(failed.preparedUpload)
            val recovered = failed.copy(options = MediaDraftOptions())
            assertSame(source.content, recovered.preparedUpload?.content)
            owner.update(listOf(recovered))
            runCurrent()
            assertEquals(1, results.size)
        }

    @Test fun cancellationDoesNotPublishAFailureOrLoseTheSelectedFile() =
        runTest {
            val file =
                ComposerAttachment(
                    ReplyAttachment("image.png", "image/png", byteArrayOf(1)),
                    options = MediaDraftOptions(uniqueHash = true),
                )
            val results = mutableListOf<List<ComposerAttachment>>()
            val owner =
                ComposerUploadPreparation(
                    backgroundScope,
                    StandardTestDispatcher(testScheduler),
                    prepare = { throw CancellationException("cancelled") },
                    publish = results::add,
                )
            owner.update(listOf(file))
            runCurrent()
            assertEquals(emptyList<List<ComposerAttachment>>(), results)
            assertNull(file.prepared)
        }
}
