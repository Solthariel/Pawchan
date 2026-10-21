package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerUploadPreparation
import com.solthariel.pawchan.composer.prepareComposerAttachments
import com.solthariel.pawchan.composer.preparedUpload
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComposerUploadPreparationTest {
    @Test fun cachesOncePerSelectionAndSendReusesTheIdenticalPreparedBytes() =
        runTest {
            val source = ReplyAttachment("file.png", "image/png", byteArrayOf(1))
            val file = ComposerAttachment(source, options = MediaDraftOptions(uniqueHash = true))
            val files = MutableStateFlow(listOf(file))
            var calls = 0
            val prepared = source.copy(content = byteArrayOf(1, 2))
            val owner =
                ComposerUploadPreparation(
                    backgroundScope,
                    StandardTestDispatcher(testScheduler),
                    prepare = {
                        calls++
                        prepared
                    },
                    publish = { files.value = it },
                )
            owner.update(files.value)
            runCurrent()
            assertEquals(1, calls)
            assertSame(prepared, files.value.single().preparedUpload)
            val sent = prepareComposerAttachments(files.value, PostingRules()) as PostingOutcome.Success
            assertSame(prepared.content, sent.value.single().content)
            runCurrent()
            assertEquals(1, calls)
            owner.update(emptyList())
        }

    @Test fun latestSelectionWinsAndOwnerCancellationPreventsPublishing() =
        runTest {
            val source = ReplyAttachment("file.png", "image/png", byteArrayOf(1))
            val file = ComposerAttachment(source, options = MediaDraftOptions(uniqueHash = true))
            val files = MutableStateFlow(listOf(file))
            val results = mutableListOf<List<ComposerAttachment>>()
            val owner =
                ComposerUploadPreparation(
                    backgroundScope,
                    StandardTestDispatcher(testScheduler),
                    prepare = { source },
                    publish = results::add,
                )
            owner.update(files.value)
            files.value = emptyList()
            owner.update(files.value)
            runCurrent()
            assertEquals(0, results.size)
            files.value = listOf(file)
            owner.update(files.value)
            owner.update(emptyList())
            runCurrent()
            assertEquals(0, results.size)
            assertNull(file.preparedUpload)
        }
}
