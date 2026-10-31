package com.solthariel.pawchan.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GithubReleaseAssetTest {
    private val asset = """{"id":123,"name":"Pawchan.apk","size":100,"state":"uploaded"}"""

    @Test fun exactUploadedAssetIsSelected() {
        assertEquals(123L, githubAssetId("""{"draft":false,"assets":[$asset]}""", "Pawchan.apk", 100))
    }

    @Test fun draftMissingDuplicateAndWrongSizeAreRejected() {
        listOf(
            """{"draft":true,"assets":[$asset]}""",
            """{"draft":false,"assets":[]}""",
            """{"draft":false,"assets":[$asset,$asset]}""",
            """{"draft":false,"assets":[$asset]}""".replace("100", "101"),
        ).forEach { assertThrows(IllegalStateException::class.java) { githubAssetId(it, "Pawchan.apk", 100) } }
    }

    @Test fun incompleteUploadAndInvalidIdAreRejected() {
        listOf(asset.replace("uploaded", "starter"), asset.replace("123", "0")).forEach {
            assertThrows(IllegalStateException::class.java) {
                githubAssetId("""{"draft":false,"assets":[$it]}""", "Pawchan.apk", 100)
            }
        }
    }
}
