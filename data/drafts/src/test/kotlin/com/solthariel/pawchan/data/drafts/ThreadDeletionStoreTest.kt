package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ThreadDeletionStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42L)),
        )

    @Test fun markerSurvivesRecreationWithoutAnyCachedPosts() =
        runTest {
            val directory = folder.newFolder()
            FileThreadDeletionStore(directory).markDeleted(address)
            assertTrue(FileThreadDeletionStore(directory).contains(address))
        }

    @Test fun sitesAndBoardsHaveIndependentMarkers() =
        runTest {
            val store = FileThreadDeletionStore(folder.newFolder())
            store.markDeleted(address)
            val otherBoard = address.copy(board = address.board.copy(board = requireNotNull(BoardId.parse("mobi"))))
            assertFalse(store.contains(otherBoard))
            val otherSite = address.copy(board = address.board.copy(site = requireNotNull(SiteId.parse("other"))))
            assertFalse(store.contains(otherSite))
        }

    @Test fun unavailableStorageDoesNotThrowOrInventADeletion() =
        runTest {
            val store = FileThreadDeletionStore(folder.newFile())
            store.markDeleted(address)
            assertFalse(store.contains(address))
        }
}
