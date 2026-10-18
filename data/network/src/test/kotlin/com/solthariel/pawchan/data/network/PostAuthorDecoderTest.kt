package com.solthariel.pawchan.data.network

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostAuthorDecoderTest {
    @Test fun decodesEntitiesAndStripsMarkup() {
        assertEquals("Аноним ID: Heaven", decodePostAuthor("Аноним&nbsp;ID:&nbsp;<span id='tag'>Heaven</span>"))
        assertEquals(
            "Глупый Дин Винчестер",
            decodePostAuthor("<span style='color:rgb(197,157,255);'>Глупый&nbsp;Дин Винчестер</span>"),
        )
        assertNull(decodePostAuthor("&nbsp;"))
    }

    @Test fun keepsValidatedAuthorColor() {
        assertEquals(0xFFC59DFF.toInt(), decodeAuthorColor("<span style='color:rgb(197,157,255);'>Имя</span>"))
        assertEquals(0xFF123456.toInt(), decodeAuthorColor("<span style='color:#123456'>Имя</span>"))
        assertNull(decodeAuthorColor("<span style='color:rgb(999,0,0)'>Имя</span>"))
    }

    @Test fun badgeUrlsStayOnDvachAndCountrySymbolsArePrepared() {
        val badges =
            decodePostBadges(
                "<img src='/flags/ru.png' title='Россия'>" +
                    "<img src='https://evil.test/tracker.png'><img src='javascript:alert(1)'>",
            )
        assertEquals(1, badges.size)
        assertEquals("🇷🇺", badges.single().symbol)
        assertEquals("https://2ch.su/flags/ru.png", badges.single().image.value)
        assertTrue(decodePostBadges(null).isEmpty())
    }

    @Test fun originalPosterAcceptsBooleanAndNumericWireForms() {
        assertTrue(decodeOriginalPoster(JsonPrimitive(true)))
        assertTrue(decodeOriginalPoster(JsonPrimitive(1)))
        assertFalse(decodeOriginalPoster(JsonPrimitive(0)))
        assertFalse(decodeOriginalPoster(null))
    }

    @Test fun serverGeneratedIdentityReplacesTheAnonymousPrefix() {
        assertEquals(
            "Глупый Дин Винчестер",
            decodePostAuthor(
                "Аноним&nbsp;ID:&nbsp;<span id='id_tag_872faeb' style='color:rgb(197,157,255);'>" +
                    "Глупый&nbsp;Дин Винчестер</span>",
            ),
        )
    }
}
