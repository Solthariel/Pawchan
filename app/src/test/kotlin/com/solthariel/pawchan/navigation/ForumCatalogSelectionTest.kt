package com.solthariel.pawchan.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForumCatalogSelectionTest {
    @Test
    fun selectsCatalogOnlyFromRequestedForum() {
        val routes = "catalog/dvach/mobi\ncatalog/4chan/g"
        assertEquals("catalog/dvach/mobi", forumCatalogRoute(routes, "dvach"))
        assertEquals("catalog/4chan/g", forumCatalogRoute(routes, "4chan"))
        assertNull(forumCatalogRoute(routes, "arhivach"))
    }
}
