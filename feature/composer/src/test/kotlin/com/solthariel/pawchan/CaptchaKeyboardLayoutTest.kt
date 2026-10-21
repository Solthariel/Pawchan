package com.solthariel.pawchan

import com.solthariel.pawchan.composer.stableCaptchaKeyboard
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CaptchaKeyboardLayoutTest {
    @Test fun permutationKeepsEightSlotsAndReusesImagesWithNewServerNumbers() {
        val before = (0..7).map { option(it, it) }
        val order = listOf(4, 0, 7, 2, 6, 3, 1, 5)
        val incoming = order.mapIndexed { index, image -> option(index, image) }
        val after = stableCaptchaKeyboard(before, incoming)
        before.forEachIndexed { index, old ->
            assertSame(old.image, after[index]?.image)
            assertEquals(order.indexOf(index), after[index]?.number)
        }
    }

    @Test fun onlyNewImagesOccupyVacatedSlots() {
        val before = (0..7).map { option(it, it) }
        val incoming = listOf(7, 9, 0, 5, 10, 3, 4, 2).mapIndexed { index, image -> option(index, image) }
        val after = stableCaptchaKeyboard(before, incoming)
        assertEquals(listOf(0, 9, 2, 3, 4, 5, 10, 7), after.map { it?.image?.first()?.toInt() })
        assertEquals(listOf(2, 1, 7, 5, 6, 3, 4, 0), after.map { it?.number })
    }

    @Test fun duplicatesAreMatchedOneToOneAndNeverLoseAnIncomingOption() {
        val before = listOf(option(0, 1), option(1, 2), option(2, 1), option(3, 3))
        val incoming = listOf(option(7, 1), option(8, 1), option(9, 1), option(10, 2))
        val after = stableCaptchaKeyboard(before, incoming)
        assertEquals(listOf(7, 10, 8, 9), after.map { it?.number })
        assertSame(before[0].image, after[0]?.image)
        assertSame(before[2].image, after[2]?.image)
        assertSame(incoming[2].image, after[3]?.image)
    }

    @Test fun shrinkLeavesEmptySlotsRatherThanShowingStaleButtonsOrMovingSurvivors() {
        val before = (0..7).map { option(it, it) }
        val after = stableCaptchaKeyboard(before, listOf(option(13, 7), option(27, 3)))
        assertEquals(8, after.size)
        assertEquals(13, after[7]?.number)
        assertEquals(27, after[3]?.number)
        assertNull(after[0])
        assertEquals(2, after.filterNotNull().size)
        val grown = stableCaptchaKeyboard(after, listOf(option(0, 7), option(1, 9), option(2, 3)))
        assertEquals(9.toByte(), grown[0]?.image?.first())
        assertEquals(0, grown[7]?.number)
        assertEquals(2, grown[3]?.number)
    }

    @Test fun hashCollisionCannotConfuseDifferentSymbols() {
        val old = EmojiCaptchaOption(0, byteArrayOf(0, 31))
        val new = EmojiCaptchaOption(1, byteArrayOf(1, 0))
        assertEquals(old.image.contentHashCode(), new.image.contentHashCode())
        val after = stableCaptchaKeyboard(listOf(old), listOf(new))
        assertSame(new.image, after.single()?.image)
        assertNotSame(old.image, after.single()?.image)
    }

    @Test fun growingKeyboardRetainsExistingSlotsAndAddsEveryNewOption() {
        val before = listOf(option(0, 1), option(1, 2))
        val incoming = listOf(3, 2, 4, 1, 5).mapIndexed { index, image -> option(index, image) }
        val after = stableCaptchaKeyboard(before, incoming)
        assertEquals(listOf(1, 2, 3, 4, 5), after.map { it?.image?.first()?.toInt() })
        assertEquals(incoming.map { it.number }.sorted(), after.filterNotNull().map { it.number }.sorted())
        assertEquals(listOf(0, 1), before.map { it.number })
    }

    @Test fun randomizedStepsPreserveTheWholeServerSetAndStableMatchingSymbols() {
        val random = Random(91)
        var before: List<EmojiCaptchaOption?> = emptyList()
        repeat(200) {
            val symbols = (0..20).shuffled(random).take(random.nextInt(1, 17))
            val incoming = symbols.mapIndexed { index, symbol -> option(index * 3 + 2, symbol) }
            val after = stableCaptchaKeyboard(before, incoming)
            assertEquals(incoming.map { it.number }.sorted(), after.filterNotNull().map { it.number }.sorted())
            before.forEachIndexed { index, old ->
                if (old != null && symbols.contains(old.image.first().toInt())) {
                    assertSame(old.image, after[index]?.image)
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
        symbol: Int,
    ): EmojiCaptchaOption = EmojiCaptchaOption(number, byteArrayOf(symbol.toByte()))
}
