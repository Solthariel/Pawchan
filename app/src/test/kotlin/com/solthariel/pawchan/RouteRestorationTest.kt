package com.solthariel.pawchan

import com.solthariel.pawchan.navigation.AppRoute
import com.solthariel.pawchan.navigation.restoredBaseBoard
import com.solthariel.pawchan.navigation.threadAddress
import com.solthariel.pawchan.workspace.validRestorableRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteRestorationTest {
    private val home = requireNotNull(threadAddress("dvach", "b", 1L)).board
    private val thread = requireNotNull(threadAddress("dvach", "vg", 100L))

    @Test fun routeValidationRejectsTransientOrInvalidScreens() {
        assertEquals(null, "challenge?url=https://example.com".validRestorableRoute())
        assertEquals("thread/dvach/b/100", "thread/dvach/b/100".validRestorableRoute())
    }

    /** Каталог всегда лежит под восстановленным экраном: доска треда, иначе стартовая. */
    @Test fun restoredScreenOpensOverItsBoardCatalog() {
        assertEquals(thread.board, restoredBaseBoard(AppRoute.thread(thread), home))
        assertEquals(thread.board, restoredBaseBoard(AppRoute.catalog(thread.board), home))
        assertEquals(home, restoredBaseBoard("settings", home))
        assertEquals(home, restoredBaseBoard(null, home))
    }

    /** Приложение закрыли на вкладке: под ней открывается доска, которую читали, а не стартовая /b/. */
    @Test fun closingOnATabKeepsTheBoardThatWasRead() {
        val read = AppRoute.catalog(thread.board)
        assertEquals(thread.board, restoredBaseBoard("settings", home, read))
        assertEquals(thread.board, restoredBaseBoard("library", home, read))
        assertEquals(home, restoredBaseBoard("library", home, "not-a-route"))
        val other = requireNotNull(threadAddress("dvach", "po", 7L))
        assertEquals(other.board, restoredBaseBoard(AppRoute.thread(other), home, read))
    }

    @Test fun archiveOpenedFromDownloadsIsRestorable() {
        assertEquals("download-archive/12", "download-archive/12".validRestorableRoute())
        assertEquals(null, "download-archive/0".validRestorableRoute())
        assertEquals("download-archive/12", AppRoute.downloadArchive(12))
    }
}
