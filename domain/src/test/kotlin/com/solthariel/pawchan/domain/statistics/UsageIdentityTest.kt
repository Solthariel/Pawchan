package com.solthariel.pawchan.domain.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UsageIdentityTest {
    @Test fun mirrorsAndFragmentsDoNotDuplicateOneRemoteFile() {
        assertEquals(
            usageMediaIdentity("https://2ch.su/b/src/1.jpg"),
            usageMediaIdentity("http://2ch.hk/b/src/1.jpg#image"),
        )
        assertEquals("https://2ch.su/b/src/1.jpg", usageMediaIdentity("https://2ch.pm:443/b/src/1.jpg"))
    }

    @Test fun otherHostsPathsAndQueriesRemainDifferent() {
        assertNotEquals(usageMediaIdentity("https://a.test/a.jpg"), usageMediaIdentity("https://b.test/a.jpg"))
        assertNotEquals(
            usageMediaIdentity("https://a.test/a.jpg?id=1"),
            usageMediaIdentity("https://a.test/a.jpg?id=2"),
        )
        assertNotEquals(
            usageMediaIdentity("https://2ch.su/b/src/1.jpg"),
            usageMediaIdentity("https://2ch.su/po/src/1.jpg"),
        )
    }

    @Test fun fileIdentityUsesBytesInsteadOfFilenameOrPostingPosition() {
        assertEquals(usageFileIdentity(byteArrayOf(1, 2)), usageFileIdentity(byteArrayOf(1, 2)))
        assertNotEquals(usageFileIdentity(byteArrayOf(1, 2)), usageFileIdentity(byteArrayOf(2, 1)))
    }

    @Test fun invalidUrlDoesNotCrashStatistics() {
        assertEquals("bad url", usageMediaIdentity(" bad url "))
    }
}
