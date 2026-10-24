package com.solthariel.pawchan.nativebuild

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/** These tests simulate compilers; they exercise orchestration, not actual NDK compilation or playback. */
class NativePipelineTest {
    @get:Rule val temp = TemporaryFolder()
    private val network = ArchiveTransport { _, _ -> error("Unexpected network") }

    @Test fun buildsVerifiedBundleAndReusesItWithoutCompilingAgain() {
        val options = Fixtures.project(temp.root.toPath())
        val sources = Fixtures.sources(temp.root.toPath().resolve("supplied"))
        val runner = SimulatedNativeTools()
        val pipeline = NativePipeline(options, runner, network)
        pipeline.execute("build", sources, temp.root.toPath().resolve("cache"))
        assertEquals(5, runner.commands.size)
        BundleManifest.verify(options.work.resolve("bundle"), options.abis)
        assertTrue(Files.exists(options.work.resolve("bundle/licenses/notice.txt")))
        pipeline.execute("build", sources, temp.root.toPath().resolve("cache"))
        assertEquals(5, runner.commands.size)
        assertFalse(Files.exists(options.root.resolve("video-ffmpeg/native-prebuilt")))
    }

    @Test fun changedRecipeAndTamperedOutputBothTriggerRebuild() {
        val options = Fixtures.project(temp.root.toPath())
        val sources = Fixtures.sources(temp.root.toPath().resolve("supplied"))
        val runner = SimulatedNativeTools()
        val pipeline = NativePipeline(options, runner, network)
        val cache = temp.root.toPath().resolve("cache")
        pipeline.execute("build", sources, cache)
        Fixtures.write(options.tool.resolve("src/main/recipe.kt"), "changed recipe")
        pipeline.execute("build", sources, cache)
        assertEquals(10, runner.commands.size)
        Fixtures.write(options.work.resolve("bundle/libraries/dav1d/arm64-v8a/libdav1d.so"), "tampered")
        pipeline.execute("build", sources, cache)
        assertEquals(15, runner.commands.size)
    }

    @Test fun failedRebuildLeavesPreviousBundleUnchanged() {
        val options = Fixtures.project(temp.root.toPath())
        val sources = Fixtures.sources(temp.root.toPath().resolve("supplied"))
        val runner = SimulatedNativeTools()
        val pipeline = NativePipeline(options, runner, network)
        val cache = temp.root.toPath().resolve("cache")
        pipeline.execute("build", sources, cache)
        val previous = FileIntegrity.tree(options.work.resolve("bundle"))
        Fixtures.write(sources.getValue("dav1d").resolve("meson.build"), "changed input")
        runner.failInstall = true
        assertThrows(IllegalStateException::class.java) { pipeline.execute("build", sources, cache) }
        assertEquals(previous, FileIntegrity.tree(options.work.resolve("bundle")))
    }

    @Test fun multipleAbisRemainSeparate() {
        val options = Fixtures.project(temp.root.toPath()).copy(abis = listOf(AndroidAbi.ARM64, AndroidAbi.X64))
        val pipeline = NativePipeline(options, SimulatedNativeTools(), network)
        pipeline.execute("build", Fixtures.sources(temp.root.toPath().resolve("supplied")), temp.root.toPath().resolve("cache"))
        BundleManifest.verify(options.work.resolve("bundle"), options.abis)
        assertTrue(Files.exists(options.work.resolve("bundle/libraries/ffmpeg/x86_64/libavcodec.so")))
    }

    @Test fun concurrentBuildIsRejectedBeforeMutatingSources() {
        val options = Fixtures.project(temp.root.toPath())
        Files.createDirectories(options.work)
        FileChannel.open(options.work.resolve("pipeline.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.lock().use {
                assertThrows(java.nio.channels.OverlappingFileLockException::class.java) {
                    NativePipeline(
                        options,
                        SimulatedNativeTools(),
                        network,
                    ).execute("prepare", emptyMap(), temp.root.toPath().resolve("cache"))
                }
            }
        }
        assertFalse(Files.exists(options.work.resolve("sources")))
    }

    @Test fun missingNdkIsRejectedBeforeAcquiringSources() {
        val options = Fixtures.project(temp.root.toPath()).copy(ndk = null)
        val pipeline = NativePipeline(options, SimulatedNativeTools(), network)
        assertThrows(IllegalArgumentException::class.java) { pipeline.execute("build", emptyMap(), temp.root.toPath().resolve("cache")) }
        assertFalse(Files.exists(options.work.resolve("sources")))
    }
}
