package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.domain.workspace.withHiddenThread
import com.solthariel.pawchan.domain.workspace.withRepliesDismissed
import com.solthariel.pawchan.domain.workspace.withSeen
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HiddenCatalogThreadsTest {
    @get:Rule val temporary = TemporaryFolder()
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(100)),
        )

    @Test
    fun hidingAndRestoringSurviveRepositoryRestart() =
        runTest {
            val directory = temporary.newFolder()
            FileWorkspaceRepository(directory).update { it.withHiddenThread(address, hide = true) }
            val restored = FileWorkspaceRepository(directory)
            assertEquals(setOf(address), restored.load().marks.hidden)
            restored.update { it.withHiddenThread(address, hide = false) }
            assertEquals(emptySet<ThreadAddress>(), FileWorkspaceRepository(directory).load().marks.hidden)
        }

    @Test
    fun priorWorkspaceVersionWithoutHiddenThreadsStillLoads() {
        val legacy = Json.parseToJsonElement(encodeWorkspace(WorkspaceState(lastRoute = "catalog"))).jsonObject
        val withoutNewField = JsonObject(legacy - "hiddenThreads").toString()
        assertEquals(WorkspaceState(lastRoute = "catalog"), decodeWorkspace(withoutNewField))
    }

    @Test
    fun seenThreadsAndDismissedRepliesSurviveEncoding() {
        val state = WorkspaceState().withSeen(address, 123L).withRepliesDismissed(address, true)
        val decoded = decodeWorkspace(encodeWorkspace(state))
        assertEquals(mapOf(address to 123L), decoded.marks.seen)
        assertEquals(setOf(address), decoded.marks.dismissedReplies)
    }
}
