package com.solthariel.pawchan

import com.solthariel.pawchan.composer.stableCaptchaKeyboard
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CaptchaKeyboardReconciliationTest {
    @Test fun emptyResponseClearsEverySlotWithoutShrinkingTheGrid() {
        val before = listOf(option(1, 8), null, option(2, 9))
        assertEquals(listOf(null, null, null), stableCaptchaKeyboard(before, emptyList()))
        assertTrue(stableCaptchaKeyboard(emptyList(), emptyList()).isEmpty())
    }

    @Test fun anInitialResponseKeepsServerOrderRatherThanImageSortOrder() {
        val incoming = listOf(option(9, 127), option(2, -128), option(5, 0))
        val after = stableCaptchaKeyboard(emptyList(), incoming)
        incoming.forEachIndexed { index, option -> assertSame(option, after[index]) }
    }

    @Test fun fewerDuplicateImagesKeepTheirEarliestPreviousSlots() {
        val before = listOf(null, option(0, 4), option(1, 4), null, option(2, 4))
        val incoming = listOf(option(91, 4), option(37, 4))
        val after = stableCaptchaKeyboard(before, incoming)
        assertEquals(listOf(null, 91, 37, null, null), after.map { it?.number })
        assertSame(before[1]?.image, after[1]?.image)
        assertSame(before[2]?.image, after[2]?.image)
    }

    @Test fun bytePrefixesAndSignedBytesCannotConfuseDifferentImages() {
        val before = listOf(option(0, -128), option(1, 0, 31), option(2, 0), option(3, 127))
        val incoming = listOf(option(40, 127), option(30, 0), option(20, 1, 0), option(10, -128))
        val after = stableCaptchaKeyboard(before, incoming)
        assertEquals(listOf(10, 20, 30, 40), after.map { it?.number })
        assertSame(before[0].image, after[0]?.image)
        assertSame(before[2].image, after[2]?.image)
        assertSame(incoming[2].image, after[1]?.image)
    }

    @Test fun inputsAndPreviouslyPublishedLayoutsAreNotMutated() {
        val old = option(4, 1, 2)
        val fresh = option(70, 1, 2)
        val before = listOf(null, old)
        val incoming = listOf(fresh)
        val after = stableCaptchaKeyboard(before, incoming)
        assertEquals(listOf(null, old), before)
        assertSame(fresh, incoming.single())
        assertNotSame(fresh, after[1])
        assertSame(old.image, after[1]?.image)
        assertEquals(4, old.number)
        assertEquals(70, fresh.number)
        assertEquals(listOf(null, 70), after.map { it?.number })
    }

    @Test fun randomizedDuplicateResponsesPreserveStableSlotsAndEveryServerNumber() {
        val random = Random(704)
        var before: List<EmojiCaptchaOption?> = emptyList()
        repeat(500) {
            val incoming = List(random.nextInt(0, 25)) { index -> option(index + 100, random.nextInt(-5, 6)) }
            val after = stableCaptchaKeyboard(before, incoming)
            assertEquals(maxOf(before.size, incoming.size), after.size)
            assertEquals(incoming.map { it.number }.sorted(), after.filterNotNull().map { it.number }.sorted())
            val available = incoming.toMutableList()
            before.forEachIndexed { index, old ->
                val match = available.indexOfFirst { old != null && it.image.contentEquals(old.image) }
                if (match >= 0) {
                    val fresh = available.removeAt(match)
                    assertEquals(fresh.number, after[index]?.number)
                    assertSame(old?.image, after[index]?.image)
                }
            }
            assertTrue(
                after.filterNotNull().all { visible ->
                    incoming.any { it.number == visible.number && it.image.contentEquals(visible.image) }
                },
            )
            before = after
        }
    }

    private fun option(
        number: Int,
        vararg bytes: Int,
    ): EmojiCaptchaOption = EmojiCaptchaOption(number, bytes.map(Int::toByte).toByteArray())
}
