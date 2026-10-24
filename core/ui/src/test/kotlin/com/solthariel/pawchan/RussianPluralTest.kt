package com.solthariel.pawchan

import com.solthariel.pawchan.core.ui.russianPlural
import org.junit.Assert.assertEquals
import org.junit.Test

class RussianPluralTest {
    private fun replies(count: Int) = russianPlural(count, "ответ", "ответа", "ответов")

    @Test
    fun declinesRepliesByLastDigits() {
        assertEquals("1 ответ", replies(1))
        assertEquals("2 ответа", replies(2))
        assertEquals("5 ответов", replies(5))
        assertEquals("11 ответов", replies(11))
        assertEquals("14 ответов", replies(14))
        assertEquals("21 ответ", replies(21))
        assertEquals("103 ответа", replies(103))
        assertEquals("112 ответов", replies(112))
    }
}
