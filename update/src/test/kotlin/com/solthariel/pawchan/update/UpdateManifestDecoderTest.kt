package com.solthariel.pawchan.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestDecoderTest {
    @Test
    fun selectsNewestClientPackage() {
        val release = UpdateManifestDecoder.decodeClientPackage(MANIFEST)

        assertEquals("1.0.1", release.versionName)
        assertEquals(2L, release.versionCode)
        assertEquals(42L, release.length)
        assertEquals("https://example.test/Pawchan.apk", release.source)
    }

    @Test
    fun comparesOnlyMonotonicVersionCode() {
        val release = UpdateManifestDecoder.decodeClientPackage(MANIFEST)

        assertTrue(UpdateChecker.isNewer(1L, release))
        assertFalse(UpdateChecker.isNewer(2L, release))
        assertFalse(UpdateChecker.isNewer(3L, release))
    }

    private companion object {
        val MANIFEST =
            """
            {
              "applications": [
                {
                  "name": "client",
                  "packages": [
                    {
                      "version_name": "1.0.0",
                      "version_code": 1,
                      "min_sdk": 30,
                      "length": 10,
                      "source": "https://example.test/old.apk",
                      "changelog": null,
                      "sha256sum": "00"
                    },
                    {
                      "version_name": "1.0.1",
                      "version_code": 2,
                      "min_sdk": 30,
                      "length": 42,
                      "source": "https://example.test/Pawchan.apk",
                      "changelog": "https://example.test/2.txt",
                      "sha256sum": "11"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
    }
}
