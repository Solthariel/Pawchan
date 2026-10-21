package com.solthariel.pawchan.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.ReadingReturnPoint
import com.solthariel.pawchan.domain.workspace.StoredReply
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import com.solthariel.pawchan.workspace.WorkspaceController
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IncomingLinkNavigationRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var controller: NavHostController
    private lateinit var scope: CoroutineScope

    @Test fun aNewThreadLinkPreservesBackNavigation() {
        showNavigation()
        compose.runOnIdle {
            controller.openIncomingLink(ForumWebLink.Thread(defaultThreadAddress(123L), null), null)
            controller.openIncomingLink(ForumWebLink.Thread(defaultThreadAddress(789L), 800L), null)
            assertEquals(defaultThreadAddress(789L), controller.currentBackStackEntry?.arguments?.toThreadAddress())
            controller.popBackStack()
            assertEquals(defaultThreadAddress(123L), controller.currentBackStackEntry?.arguments?.toThreadAddress())
        }
    }

    @Test fun postLinkToAnAlreadyOpenThreadRecreatesTheReadingAnchor() {
        showNavigation()
        compose.runOnIdle {
            controller.navigateThread(defaultThreadAddress(123L))
            val before = controller.currentBackStackEntry
            controller.openIncomingLink(ForumWebLink.Thread(defaultThreadAddress(123L), 456L), null)
            assertNotSame(before, controller.currentBackStackEntry)
            controller.popBackStack()
            assertEquals(AppRoute.CATALOG, controller.currentBackStackEntry?.destination?.route)
        }
    }

    @Test fun boardLinkReplacesTheCatalogWithoutLeavingTheApp() {
        showNavigation()
        compose.runOnIdle {
            val board = requireNotNull(boardAddress("dvach", "mobi"))
            controller.openIncomingLink(ForumWebLink.Board(board), null)
            assertEquals(board, controller.currentBackStackEntry?.arguments?.toBoardAddress())
        }
    }

    @Test fun replyNotificationOpensFirstUnreadAndPreservesThePreviousReadingPlace() {
        showNavigation()
        val workspace = readingWorkspace()
        val address = defaultThreadAddress(123L)
        compose.runOnIdle {
            controller.navigateThread(address)
            val before = controller.currentBackStackEntry
            val link = requireNotNull(incomingForumLink("pawchan://thread/dvach/b/123?replies=1"))
            controller.openIncomingLink(link, workspace)
            assertEquals(300L, workspace.requestedPost?.postId)
            assertNotSame(before, controller.currentBackStackEntry)
        }
        compose.waitUntil(5_000) { workspace.state.value.marks.readingReturns[address] != null }
        compose.runOnIdle {
            assertEquals(ReadingReturnPoint(150, 23), workspace.state.value.marks.readingReturns[address])
            assertEquals(
                150L,
                workspace.state.value.tabs
                    .single()
                    .postId,
            )
            controller.popBackStack()
            assertEquals(AppRoute.CATALOG, controller.currentBackStackEntry?.destination?.route)
        }
    }

    @Test fun ordinaryWatchNotificationKeepsTheNormalOpeningBehavior() {
        showNavigation()
        val workspace = readingWorkspace()
        compose.runOnIdle {
            val link = requireNotNull(incomingForumLink("pawchan://thread/dvach/b/123"))
            controller.openIncomingLink(link, workspace)
            assertNull(workspace.requestedPost)
            assertTrue(
                workspace.state.value.marks.readingReturns
                    .isEmpty(),
            )
        }
    }

    private fun readingWorkspace(): WorkspaceController {
        val address = defaultThreadAddress(123L)
        val replies =
            listOf(400L, 300L).map { id ->
                StoredReply(
                    BoardPost(
                        PostAddress(address, requireNotNull(PostId.parse(id))),
                        PostContent(PostDocument.plain("Reply"), PostMetadata(null, null, false)),
                        emptyList(),
                    ),
                    "Thread",
                )
            }
        val initial = WorkspaceState(tabs = listOf(OpenThread(address, "Thread", 150, 23)), replies = replies)
        lateinit var workspace: WorkspaceController
        compose.runOnIdle { workspace = WorkspaceController(scope, NavigationReadingStorage(initial)) }
        compose.waitUntil(5_000) { workspace.ready.value }
        return workspace
    }

    private fun showNavigation() {
        compose.setContent {
            scope = rememberCoroutineScope()
            controller = rememberNavController()
            NavHost(controller, startDestination = AppRoute.catalog(defaultBoardAddress())) {
                composable(AppRoute.CATALOG, arguments = boardArguments()) { Text("Каталог") }
                composable(AppRoute.THREAD, arguments = threadArguments()) { Text("Тред") }
            }
        }
        compose.waitForIdle()
    }
}

private class NavigationReadingStorage(
    private var state: WorkspaceState,
) : WorkspaceRepository {
    override suspend fun load(): WorkspaceState = state

    override suspend fun update(transform: (WorkspaceState) -> WorkspaceState): WorkspaceState {
        state = transform(state)
        return state
    }
}
