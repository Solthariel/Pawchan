package com.solthariel.pawchan.nativebuild

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class NativeCliTest {
    @get:Rule val temp = TemporaryFolder()

    private fun parse(vararg args: String): CliRequest = NativeCli.parse(arrayOf(*args), emptyMap(), temp.root.toPath())

    @Test fun planHasArm64DefaultAndDoesNotCreateOutputs() {
        val request = parse("plan", "--project-root", ".")
        assertEquals(listOf(AndroidAbi.ARM64), request.options.abis)
        assertEquals(4, request.options.jobs)
        assertFalse(Files.exists(request.options.work))
    }

    @Test fun explicitMultipleAbisAndOfflineAreAccepted() {
        val request = parse("prepare", "--project-root", ".", "--abi", "arm64-v8a,x86_64", "--offline", "--jobs", "2")
        assertEquals(listOf(AndroidAbi.ARM64, AndroidAbi.X64), request.options.abis)
        assertTrue(request.options.offline)
        assertEquals(2, request.options.jobs)
    }

    @Test fun unknownAndDuplicateOptionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { parse("build", "--typo", "value") }
        assertThrows(IllegalArgumentException::class.java) { parse("build", "--jobs", "2", "--jobs", "3") }
    }

    @Test fun missingValuesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { parse("build", "--ndk") }
    }

    @Test fun invalidJobsAndAbisAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { parse("build", "--project-root", ".", "--jobs", "0") }
        assertThrows(IllegalStateException::class.java) { parse("build", "--project-root", ".", "--abi", "mips") }
        assertThrows(IllegalArgumentException::class.java) { parse("build", "--project-root", ".", "--abi", "arm64-v8a,arm64-v8a") }
    }

    @Test fun legacySourceEnvironmentRemainsCompatible() {
        val request =
            NativeCli.parse(
                arrayOf("prepare", "--project-root", "."),
                mapOf(
                    "NEKOCHAN_FFMPEG_SOURCE_DIR" to "source with spaces",
                    "ANDROID_NDK_HOME" to "ndk",
                    "WEBM_ARCHIVE_CACHE_DIR" to "cache",
                ),
                temp.root.toPath(),
            )
        assertEquals(temp.root.toPath().resolve("source with spaces"), request.sources["ffmpeg"])
        assertEquals(temp.root.toPath().resolve("ndk"), request.options.ndk)
        assertEquals(temp.root.toPath().resolve("cache"), request.cache)
    }

    @Test fun explicitArgumentsTakePrecedenceOverEnvironment() {
        val request =
            NativeCli.parse(
                arrayOf("build", "--project-root", ".", "--abi", "x86_64", "--ndk", "chosen"),
                mapOf("NATIVE_ABIS" to "arm64-v8a", "ANDROID_NDK_HOME" to "ignored"),
                temp.root.toPath(),
            )
        assertEquals(listOf(AndroidAbi.X64), request.options.abis)
        assertEquals(temp.root.toPath().resolve("chosen"), request.options.ndk)
    }

    @Test fun unknownCommandIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { parse("publish", "--project-root", ".") }
    }
}
