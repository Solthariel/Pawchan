package com.solthariel.pawchan

import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostStateMarker
import com.solthariel.pawchan.thread.ThreadPostByline
import com.solthariel.pawchan.thread.exportDocument
import com.solthariel.pawchan.thread.shotDisplayHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ThreadShotHeaderPreferencesTest {
    @Test fun shotUsesChosenTimezoneAndYearSetting() {
        val base = toolsPost(7)
        val timestamp = Instant.parse("2020-01-02T12:34:56Z").epochSecond
        val post =
            base.copy(
                metadata =
                    base.metadata.copy(
                        display =
                            base.metadata.display.copy(
                                byline = ThreadPostByline(timestamp = timestamp),
                            ),
                    ),
            )
        val preferences = PostDisplayPreferences(zone = ZoneId.of("Asia/Yekaterinburg"))
        assertEquals("#7 02.01.20 17:34:56", post.shotDisplayHeader(preferences).text)
        assertEquals(
            "#7 02.01.20 17:34:56",
            post.shotDisplayHeader(preferences).text,
        )
        assertEquals(
            "#7 02.01.20 15:34:56",
            post.shotDisplayHeader(preferences.copy(zone = ZoneId.of("Europe/Moscow"))).text,
        )
    }

    @Test fun shotRespectsAuthorAndMarkerPreferencesWithoutChangingHtmlHeader() {
        val base = toolsPost(7, text = "https://vocaroo.com/example")
        val post =
            base.copy(
                metadata =
                    base.metadata.copy(
                        display =
                            base.metadata.display.copy(
                                byline = ThreadPostByline(date = "02.01 12:34:56", defaultName = "Аноним"),
                                markers = listOf(PostStateMarker.ORIGINAL_POSTER, PostStateMarker.SAGE),
                            ),
                    ),
            )
        val preferences = PostDisplayPreferences(showDefaultName = true)
        val header = post.shotDisplayHeader(preferences).text
        assertTrue(header.contains("OP sage Аноним"))
        val hidden = post.shotDisplayHeader(preferences.copy(showStateMarkers = false, showDefaultName = false)).text
        assertFalse(hidden.contains("OP"))
        assertFalse(hidden.contains("Аноним"))
        val content = toolsContent(listOf(post))
        assertEquals(
            post.metadata.display.header,
            content
                .exportDocument()
                .posts
                .single()
                .header,
        )
        assertEquals(
            header,
            content
                .exportDocument(setOf(7L), preferences)
                .posts
                .single()
                .header.text,
        )
        assertEquals(
            "https://vocaroo.com/example",
            content
                .exportDocument(setOf(7L), preferences)
                .posts
                .single()
                .document.plainText,
        )
    }
}
