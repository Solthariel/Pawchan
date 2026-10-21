package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.workspace.ReadingReturnPoint
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.domain.workspace.withReadingReturn
import com.solthariel.pawchan.domain.workspace.withoutReadingReturn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReadingReturnPersistenceTest {
    @get:Rule val temporary = TemporaryFolder()
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42)),
        )
    private val point = ReadingReturnPoint(150, 23)

    @Test fun aNewRepositoryInstanceRestoresPostOffsetAndFullSiteIdentity() =
        runTest {
            val directory = temporary.newFolder()
            val other = address.copy(board = address.board.copy(site = requireNotNull(SiteId.parse("other"))))
            FileWorkspaceRepository(directory).update {
                it.withReadingReturn(address, point).withReadingReturn(other, ReadingReturnPoint(150, 99))
            }
            val loaded = FileWorkspaceRepository(directory).load()
            assertEquals(point, loaded.marks.readingReturns[address])
            assertEquals(ReadingReturnPoint(150, 99), loaded.marks.readingReturns[other])
            FileWorkspaceRepository(directory).update { it.withoutReadingReturn(address, point) }
            val returned = FileWorkspaceRepository(directory).load()
            assertNull(returned.marks.readingReturns[address])
            assertEquals(ReadingReturnPoint(150, 99), returned.marks.readingReturns[other])
        }

    @Test fun oldWorkspaceWithoutTheOptionalFieldRemainsReadable() {
        val source = encodeWorkspace(WorkspaceState()).replace("\"readingReturns\":[],", "")
        assertTrue(decodeWorkspace(source).marks.readingReturns.isEmpty())
    }

    @Test fun repeatedJumpAndStaleCompletionNeverReplaceOrRemoveTheAnchor() {
        val saved = WorkspaceState().withReadingReturn(address, point)
        val next = ReadingReturnPoint(300, 0)
        assertEquals(saved, saved.withReadingReturn(address, next))
        assertEquals(saved, saved.withoutReadingReturn(address, next))
        assertTrue(
            saved
                .withoutReadingReturn(address, point)
                .marks.readingReturns
                .isEmpty(),
        )
    }

    @Test fun transferDoesNotExportPrivateViewportReturnPoints() {
        val saved = WorkspaceState().withReadingReturn(address, point)
        assertTrue(
            WorkspaceTransfer
                .decode(WorkspaceTransfer.encode(saved))
                .marks.readingReturns
                .isEmpty(),
        )
        assertEquals(saved, WorkspaceTransfer.merge(saved, WorkspaceState()))
    }
}
