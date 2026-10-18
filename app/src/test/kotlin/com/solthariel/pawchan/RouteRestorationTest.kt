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
}
