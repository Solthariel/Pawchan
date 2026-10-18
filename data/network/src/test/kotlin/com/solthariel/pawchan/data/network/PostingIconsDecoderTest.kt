package com.solthariel.pawchan.data.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostingIconsDecoderTest {
    @Test
    fun keepsServerOrderInsteadOfSortingNumbersOrNames() {
        val icons =
            decode(
                """{"enable_icons":true,"icons":[{"num":7,"name":"Рено","url":"/static/icons/renault.png"},
                    {"num":1,"name":"Альфа Ромео"}]}""",
            )
        assertEquals(listOf(7, 1), icons.map { it.id })
        assertEquals(listOf("Рено", "Альфа Ромео"), icons.map { it.name })
        assertEquals("https://2ch.su/static/icons/renault.png", icons.first().imageUrl)
        assertEquals(null, icons.last().imageUrl)
    }

    @Test
    fun disabledMissingOrEmptyBoardIconsDoNotCreateASelector() {
        assertTrue(decode("""{"enable_icons":false,"icons":[{"num":1,"name":"test"}]}""").isEmpty())
        assertTrue(decode("""{"icons":[{"num":1,"name":"test"}]}""").isEmpty())
        assertTrue(decode("""{"enable_icons":true,"icons":[]}""").isEmpty())
    }

    @Test
    fun rejectsMalformedItemsAndKeepsFirstDuplicateInPlace() {
        val icons =
            decode(
                """{"enable_icons":1,"icons":[null,{}, {"num":-1,"name":"none"},
            {"num":2,"name":""}, {"num":3,"name":"First"},{"num":3,"name":"Second"}]}""",
            )
        assertEquals(1, icons.size)
        assertEquals("First", icons.single().name)
    }

    private fun decode(value: String) = decodePostingIcons(Json.parseToJsonElement(value) as JsonObject)
}
