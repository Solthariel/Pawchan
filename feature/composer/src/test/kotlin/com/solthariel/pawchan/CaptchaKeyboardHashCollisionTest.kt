package com.solthariel.pawchan

import com.solthariel.pawchan.composer.stableCaptchaKeyboard
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CaptchaKeyboardHashCollisionTest {
    @Test
    fun collidingImagesRetainTheirOwnSlotsAfterReordering() {
        val before = listOf(option(0, 0), option(1, 1), option(2, 2))
        assertEquals(1, before.map { it.image.contentHashCode() }.distinct().size)
        val incoming = listOf(option(70, 2), option(71, 0), option(72, 1))
        val after = stableCaptchaKeyboard(before, incoming)
        assertEquals(listOf(71, 72, 70), after.map { it?.number })
        before.forEachIndexed { index, old -> assertSame(old.image, after[index]?.image) }
    }

    @Test
    fun duplicateAndDistinctImagesSharingOneHashAreConsumedOnlyOnce() {
        val before = listOf(null, option(0, 0), option(1, 1), option(2, 0))
        val incoming = listOf(option(90, 1), option(91, 0), option(92, 1), option(93, 0), option(94, 0))
        val after = stableCaptchaKeyboard(before, incoming)
        assertEquals(listOf(92, 91, 90, 93, 94), after.map { it?.number })
        assertSame(before[1]?.image, after[1]?.image)
        assertSame(before[2]?.image, after[2]?.image)
        assertSame(before[3]?.image, after[3]?.image)
        assertSame(incoming[2].image, after[0]?.image)
        assertSame(incoming[4].image, after[4]?.image)
    }

    @Test
    fun removedCollidingImageReturnsAsNewWithoutRevivingItsOldNumber() {
        val before = listOf(option(0, 0), option(1, 1), option(2, 2))
        val reduced = stableCaptchaKeyboard(before, listOf(option(10, 1), option(11, 2)))
        assertEquals(listOf(null, 10, 11), reduced.map { it?.number })
        val incoming = listOf(option(20, 0), option(21, 1), option(22, 2))
        val after = stableCaptchaKeyboard(reduced, incoming)
        assertEquals(listOf(20, 21, 22), after.map { it?.number })
        assertSame(incoming[0].image, after[0]?.image)
        assertSame(before[1].image, after[1]?.image)
        assertSame(before[2].image, after[2]?.image)
    }

    @Test
    fun randomizedCollisionBucketsPreserveSlotsDuplicatesAndServerNumbers() {
        val random = Random(936)
        var before: List<EmojiCaptchaOption?> = emptyList()
        repeat(500) {
            val incoming = List(random.nextInt(0, 25)) { index -> option(index + 100, random.nextInt(3)) }
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
        symbol: Int,
    ): EmojiCaptchaOption = EmojiCaptchaOption(number, byteArrayOf(symbol.toByte(), (62 - symbol * 31).toByte()))
}
