package com.solthariel.pawchan.infrastructure.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadGrantPolicyTest {
    private val old = "content://com.android.externalstorage.documents/tree/primary%3AOld"
    private val current = "content://com.android.externalstorage.documents/tree/primary%3ANew"
    private val destination = "$old/document/primary%3AOld%2Ffile.jpg"

    @Test fun unusedOldTreeCanBeReleasedButCurrentTreeCannot() {
        assertTrue(canReleaseDownloadGrant(old, current, emptyList(), emptySet()))
        assertFalse(canReleaseDownloadGrant(current, current, emptyList(), emptySet()))
    }

    @Test fun queuedRunningAndRetryingDownloadsKeepTheirFolder() {
        assertFalse(canReleaseDownloadGrant(old, current, listOf(destination), emptySet()))
        val active = "$current/document/primary%3ANew%2Ffile.jpg"
        assertTrue(canReleaseDownloadGrant(old, current, listOf(active), emptySet()))
    }

    @Test fun documentCreationAndUncommittedEnqueueKeepTheirFolder() {
        assertFalse(canReleaseDownloadGrant(old, current, emptyList(), setOf(old)))
        assertFalse(canReleaseDownloadGrant(old, current, emptyList(), setOf("$old%2Fchild")))
    }

    @Test fun unresolvedActiveDestinationConservativelyKeepsPermissions() {
        assertFalse(canReleaseDownloadGrant(old, current, listOf(null), emptySet()))
        assertFalse(canReleaseDownloadGrant(old, current, listOf("invalid"), emptySet()))
    }

    @Test fun similarNamesAndDifferentProvidersDoNotCountAsTheOldFolder() {
        assertFalse(downloadGrantCovers(old, "$old/document/primary%3AOld2%2Ffile.jpg"))
        assertFalse(downloadGrantCovers(old, destination.replace("externalstorage", "other")))
    }

    @Test fun directDocumentUrisAndNestedDocumentsAreRecognized() {
        val direct = "content://com.android.externalstorage.documents/document/primary%3AOld%2Fa.jpg"
        assertTrue(downloadGrantCovers(old, direct))
        assertTrue(downloadGrantCovers(old, "$old/document/primary%3AOld%2Fchild%2Fa.jpg"))
    }

    @Test fun literalPlusAndEncodedSeparatorsAreNotMisread() {
        val plus = "content://provider/tree/primary%3AA+B"
        assertTrue(downloadGrantCovers(plus, "content://provider/document/primary%3AA%2BB%2Fx.jpg"))
        assertFalse(downloadGrantCovers(plus, "content://provider/document/primary%3AA%20B%2Fx.jpg"))
    }
}
