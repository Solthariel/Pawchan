package com.solthariel.pawchan.nativebuild

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class BundleManifestTest {
    @get:Rule val temp = TemporaryFolder()

    private fun bundle(): Path {
        val options = Fixtures.project(temp.root.toPath())
        val pipeline = NativePipeline(options, SimulatedNativeTools(), ArchiveTransport { _, _ -> error("Unexpected network") })
        pipeline.execute("build", Fixtures.sources(temp.root.toPath().resolve("supplied")), temp.root.toPath().resolve("cache"))
        return options.work.resolve("bundle")
    }

    @Test fun missingLibraryIsRejected() {
        val root = bundle()
        Files.delete(root.resolve("libraries/ffmpeg/arm64-v8a/libavcodec.so"))
        assertThrows(IllegalStateException::class.java) { BundleManifest.verify(root, listOf(AndroidAbi.ARM64)) }
    }

    @Test fun changedFileIsRejectedEvenWhenNameIsCorrect() {
        val root = bundle()
        Fixtures.write(root.resolve("libraries/ffmpeg/arm64-v8a/libavcodec.so"), "changed library")
        assertThrows(IllegalStateException::class.java) { BundleManifest.verify(root, listOf(AndroidAbi.ARM64)) }
    }

    @Test fun unexpectedFileCannotSilentlyEnterBundle() {
        val root = bundle()
        Fixtures.write(root.resolve("libraries/extra.so"), "extra")
        assertThrows(IllegalStateException::class.java) { BundleManifest.verify(root, listOf(AndroidAbi.ARM64)) }
    }

    @Test fun wrongAbiAndOldInputFingerprintAreRejected() {
        val root = bundle()
        assertThrows(IllegalStateException::class.java) { BundleManifest.verify(root, listOf(AndroidAbi.X64)) }
        assertThrows(IllegalStateException::class.java) { BundleManifest.verify(root, listOf(AndroidAbi.ARM64), "old fingerprint") }
    }

    @Test fun manifestWithEscapingPathIsRejected() {
        val root = bundle()
        val manifest = root.resolve("manifest.json")
        val text = Files.readString(manifest)
        assertTrue(text.contains("licenses/notice.txt"))
        Files.writeString(manifest, text.replace("licenses/notice.txt", "../../outside"))
        assertThrows(IllegalStateException::class.java) { BundleManifest.verify(root, listOf(AndroidAbi.ARM64)) }
    }
}
