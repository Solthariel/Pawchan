package com.solthariel.pawchan.nativebuild

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeNoException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.FileSystemException
import java.nio.file.Files

class SourceStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private val bytes = Fixtures.archive("upstream/configure")

    private fun store(
        transport: ArchiveTransport = ArchiveTransport { _, output -> Files.write(output, bytes) },
        runner: CommandRunner =
            CommandRunner {
                error("Unexpected process")
            },
    ): SourceStore =
        SourceStore(temp.root.toPath().resolve("sources"), ArchiveCache(temp.root.toPath().resolve("cache"), transport), runner)

    @Test fun providedSourceIsCopiedNotAliasedAndGitMetadataIsExcluded() {
        val provided = temp.root.toPath().resolve("provided")
        Fixtures.write(provided.resolve("configure"), "original")
        Fixtures.write(provided.resolve(".git/config"), "private VCS data")
        val target = store().prepare(Fixtures.dependency(bytes), provided, true)
        assertEquals("original", Files.readString(target.resolve("configure")))
        assertFalse(Files.exists(target.resolve(".git")))
        Fixtures.write(target.resolve("configure"), "modified copy")
        assertEquals("original", Files.readString(provided.resolve("configure")))
    }

    @Test fun changedProvidedSourceInvalidatesPreparedCopy() {
        val provided = temp.root.toPath().resolve("provided")
        Fixtures.write(provided.resolve("configure"), "before")
        val store = store()
        val target = store.prepare(Fixtures.dependency(bytes), provided, true)
        Fixtures.write(provided.resolve("configure"), "after")
        store.prepare(Fixtures.dependency(bytes), provided, true)
        assertEquals("after", Files.readString(target.resolve("configure")))
    }

    @Test fun corruptedPreparedTreeIsRecreatedFromVerifiedCacheOffline() {
        val store = store()
        val spec = Fixtures.dependency(bytes)
        val target = store.prepare(spec, null, false)
        Fixtures.write(target.resolve("configure"), "tamper")
        store.prepare(spec, null, true)
        assertEquals("AAAA", Files.readString(target.resolve("configure")))
    }

    @Test fun failedReplacementPreservesWorkingSource() {
        val store = store()
        val spec = Fixtures.dependency(bytes)
        val target = store.prepare(spec, null, false)
        val invalid = temp.root.toPath().resolve("invalid")
        Fixtures.write(invalid.resolve("not-configure"), "bad source")
        assertThrows(IllegalArgumentException::class.java) { store.prepare(spec, invalid, true) }
        assertEquals("AAAA", Files.readString(target.resolve("configure")))
    }

    @Test fun overlappingSourceIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { store().prepare(Fixtures.dependency(bytes), temp.root.toPath(), true) }
    }

    @Test fun symbolicLocalSourceIsRejected() {
        val provided = temp.root.toPath().resolve("provided")
        Fixtures.write(provided.resolve("configure"), "source")
        try {
            Files.createSymbolicLink(provided.resolve("escape"), provided.resolve("configure"))
        } catch (failure: FileSystemException) {
            assumeNoException("Symbolic links require filesystem support and Windows privileges", failure)
        } catch (failure: UnsupportedOperationException) {
            assumeNoException("Filesystem does not support symbolic links", failure)
        }
        assertThrows(IllegalArgumentException::class.java) { store().prepare(Fixtures.dependency(bytes), provided, true) }
    }

    @Test fun networkUnavailabilityCanUsePinnedGitCommit() {
        val git = GitSource("https://example.test/repo", "refs/tags/version", "a".repeat(40))
        val runner =
            CommandRunner { command ->
                if ("checkout" in command.arguments) Fixtures.write(command.directory.resolve("configure"), "git source")
                if ("rev-parse" in command.arguments) git.commit else ""
            }
        val store = store(ArchiveTransport { _, _ -> throw SourceUnavailable("unavailable") }, runner)
        assertEquals("git source", Files.readString(store.prepare(Fixtures.dependency(bytes, git), null, false).resolve("configure")))
    }

    @Test fun mismatchedGitCommitIsNotPublished() {
        val git = GitSource("https://example.test/repo", "refs/tags/version", "a".repeat(40))
        val store = store(ArchiveTransport { _, _ -> throw SourceUnavailable("unavailable") }, CommandRunner { "b".repeat(40) })
        assertThrows(IntegrityFailure::class.java) { store.prepare(Fixtures.dependency(bytes, git), null, false) }
        assertFalse(Files.exists(temp.root.toPath().resolve("sources/ffmpeg")))
    }

    @Test fun checksumFailureNeverFallsBackToGit() {
        val git = GitSource("https://example.test/repo", "refs/tags/version", "a".repeat(40))
        val store = store(ArchiveTransport { _, output -> Fixtures.write(output, "wrong bytes") })
        assertThrows(IntegrityFailure::class.java) { store.prepare(Fixtures.dependency(bytes, git), null, false) }
    }

    @Test fun offlineMissNeverFallsBackToGit() {
        val git = GitSource("https://example.test/repo", "refs/tags/version", "a".repeat(40))
        assertThrows(SourceUnavailable::class.java) { store().prepare(Fixtures.dependency(bytes, git), null, true) }
    }
}
