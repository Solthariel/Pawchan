package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.AccountUploadLimits
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountUploadLimitsTest {
    private val decoder = PostingPayloadDecoder(Json)

    @Test
    fun verifiedPassUsesCamelCaseLimitsAndKibibyteSize() {
        val result = skip("""{"result":2,"maxFiles":8,"maxFilesSize":30720}""")
        assertEquals(AccountUploadLimits(8, 30 * 1024 * 1024), result.uploadLimits)
    }

    @Test
    fun disabledCaptchaCannotGrantPaidLimits() {
        assertNull(skip("""{"result":3,"maxFiles":8,"maxFilesSize":30720}""").uploadLimits)
    }

    @Test
    fun missingOrInvalidLimitsDoNotInventEntitlement() {
        for (payload in listOf(
            """{"result":2}""",
            """{"result":2,"maxFiles":0,"maxFilesSize":30720}""",
            """{"result":2,"maxFiles":8,"maxFilesSize":-1}""",
            """{"result":2,"max_files":8,"max_files_size":30720}""",
        )) {
            assertNull(skip(payload).uploadLimits)
        }
    }

    @Test
    fun excessiveServerLimitsCannotOverflowLocalSafetyBudget() {
        assertEquals(
            AccountUploadLimits(8, ReplyAttachment.MAX_ATTACHMENT_BYTES),
            skip("""{"result":2,"maxFiles":100,"maxFilesSize":9223372036854775807}""").uploadLimits,
        )
    }

    private fun skip(payload: String): CaptchaIdResult.Skip =
        (decoder.decodeCaptchaId(payload) as PostingOutcome.Success).value as CaptchaIdResult.Skip
}
