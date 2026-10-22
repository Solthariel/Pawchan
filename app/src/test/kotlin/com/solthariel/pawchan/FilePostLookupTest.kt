package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.AttachmentId
import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.infrastructure.sites.DvachWebLinks
import com.solthariel.pawchan.infrastructure.thread.AppPostLinkResolver
import com.solthariel.pawchan.infrastructure.thread.FilePostLookup
import com.solthariel.pawchan.infrastructure.thread.findFilePost
import com.solthariel.pawchan.navigation.threadAddress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilePostLookupTest {
    private val address = requireNotNull(threadAddress("dvach", "test", 237957))
    private val resolver = AppPostLinkResolver(listOf(DvachWebLinks()))
    private val samples =
        listOf(
            238116L to "17847494512720806312.mp4",
            238117L to "17847495108690174855.mp4",
            238119L to "17847497729470456047.mp4",
            238121L to "17847499404920501303.mp4",
            238123L to "17847512194020783631.webm",
            238124L to "17847513072380332637.webm",
        )

    @Test fun allSixLiveLinksFindTheirPostsInDatedArchive() =
        runTest {
            val repository = repository(samples.map { (post, file) -> post(post, archive(file)) })
            samples.forEach { (post, file) ->
                assertEquals(FilePostLookup.Found(post), repository.findFilePost(address, live(file), ::key))
            }
        }

    @Test fun archiveLinksFindLiveAttachmentsAndRetainThreadAction() =
        runTest {
            val (post, file) = samples.first()
            val repository = repository(listOf(post(post, live(file))))
            assertEquals(FilePostLookup.Found(post), repository.findFilePost(address, archive(file), ::key))
            val target = resolver.resolve(PostLink.External(requireNotNull(RemoteUrl.parse(archive(file)))), null)
            assertEquals(address, target?.fileThread)
            assertEquals(MediaKind.VIDEO, target?.attachment?.kind)
        }

    @Test fun mirrorsQueriesAndUndatedArchiveKeepTheSameIdentity() {
        val file = samples.first().second
        listOf(
            "https://2ch.hk/test/src/237957/$file?download=1#x",
            "https://2ch.life/test/arch/2026-10-18/src/237957/$file",
            "https://2ch.su/test/arch/src/237957/$file",
        ).forEach { assertEquals(key(live(file)), key(it)) }
    }

    @Test fun boardThreadAndFullFilenameCannotCollide() {
        val url = live(samples.first().second)
        listOf(
            url.replace("/test/", "/b/"),
            url.replace("/237957/", "/237958/"),
            url.replace(".mp4", ".webm"),
        ).forEach { assertNotEquals(key(url), key(it)) }
    }

    @Test fun foreignOriginsThumbnailsAndMalformedPathsAreRejected() {
        val url = live(samples.first().second)
        listOf(
            url.replace("2ch.su", "2ch.su.evil.test"),
            url.replace("2ch.su", "user@2ch.su"),
            url.replace("2ch.su", "2ch.su:444"),
            url.replace("https:", "http:"),
            url.replace("/src/", "/thumb/"),
            url.replace("/src/", "/arch/not-a-date/src/"),
            url.replace("/src/", "/arch/2026-10-18/thumb/"),
            "not a URI",
        ).forEach { assertNull(it, key(it)) }
        val other = requireNotNull(threadAddress("other", "test", 237957))
        assertNull(resolver.filePath(other, url))
    }

    @Test fun missingFileDoesNotBecomeAFalseMatch() =
        runTest {
            val repository = repository(listOf(post(238116, archive("different.mp4"))))
            assertEquals(FilePostLookup.Missing, repository.findFilePost(address, live(samples.first().second), ::key))
        }

    @Test fun originalPostStillHasAnExplicitScrollTarget() =
        runTest {
            val file = samples.first().second
            val repository = repository(listOf(post(address.thread.value, archive(file))))
            assertEquals(
                FilePostLookup.Found(address.thread.value),
                repository.findFilePost(address, live(file), ::key),
            )
        }

    @Test fun repositoryFailureIsNotReportedAsMissingFile() =
        runTest {
            val repository =
                object : ThreadRepository {
                    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
                        DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
                }
            assertEquals(
                FilePostLookup.Unavailable,
                repository.findFilePost(address, live(samples.first().second), ::key),
            )
            assertEquals(FilePostLookup.Missing, repository.findFilePost(address, "not a URI", ::key))
        }

    private fun key(url: String): String? = resolver.filePath(address, url)

    private fun post(
        number: Long,
        url: String,
    ): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(number))),
            PostContent(PostDocument.plain("test"), PostMetadata(null, null, false)),
            listOf(
                MediaAttachment(
                    requireNotNull(AttachmentId.parse(url)),
                    url.substringAfterLast('/'),
                    AttachmentSource(requireNotNull(RemoteUrl.parse(url)), null),
                    null,
                    MediaKind.VIDEO,
                ),
            ),
        )

    private fun repository(posts: List<BoardPost>): ThreadRepository =
        object : ThreadRepository {
            override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
                DomainOutcome.Success(ThreadSnapshot(address, null, ThreadFlags(false, true, true), posts))
        }
}

private fun live(file: String): String = "https://2ch.su/test/src/237957/$file"

private fun archive(file: String): String = "https://2ch.su/test/arch/2026-10-18/src/237957/$file"
