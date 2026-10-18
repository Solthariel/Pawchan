package com.solthariel.pawchan

import com.solthariel.pawchan.thread.vocaroo.formatVocarooTime
import com.solthariel.pawchan.thread.vocaroo.vocarooId
import com.solthariel.pawchan.thread.vocaroo.vocarooMp3Url
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VocarooLinksTest {
    @Test fun recognizesRecordingLinks() {
        assertEquals("1jWQcYSTTahH", vocarooId("https://vocaroo.com/1jWQcYSTTahH"))
        assertEquals("1jWQcYSTTahH", vocarooId("https://www.vocaroo.com/1jWQcYSTTahH"))
        assertEquals("1jWQcYSTTahH", vocarooId("https://voca.ro/1jWQcYSTTahH"))
        assertEquals("1jWQcYSTTahH", vocarooId("https://vocaroo.com/embed/1jWQcYSTTahH?autoplay=0"))
        assertEquals("https://media.vocaroo.com/mp3/1jWQcYSTTahH", vocarooMp3Url("1jWQcYSTTahH"))
    }

    @Test fun ignoresOtherPagesAndHosts() {
        assertNull(vocarooId("https://vocaroo.com/"))
        assertNull(vocarooId("https://vocaroo.com/upload"))
        assertNull(vocarooId("https://vocaroo.com.evil.org/1jWQcYSTTahH"))
        assertNull(vocarooId("https://example.com/1jWQcYSTTahH"))
        assertNull(vocarooId("https://vocaroo.com/a/b/c"))
    }

    @Test fun formatsTime() {
        assertEquals("0:00", formatVocarooTime(0))
        assertEquals("0:49", formatVocarooTime(49_900))
        assertEquals("12:05", formatVocarooTime(725_000))
    }
}
