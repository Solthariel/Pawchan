package com.solthariel.pawchan.settings

import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterRuleToggleTest {
    private val first = newContentFilterRule(defaultBoardAddress(), "кот").copy(id = "first")
    private val second = newContentFilterRule(defaultBoardAddress(), "пёс").copy(id = "second")

    @Test fun disablingChangesOnlyTheChosenRuleAndKeepsOrder() {
        val next = listOf(first, second).withRuleEnabled("second", false)

        assertEquals(listOf("first", "second"), next.map { it.id })
        assertTrue(next[0].enabled)
        assertFalse(next[1].enabled)
        assertEquals(second.copy(enabled = false), next[1])
        assertSame(first, next[0])
    }

    @Test fun enablingRestoresTheRuleWithoutTouchingConditions() {
        val disabled = second.copy(enabled = false)

        val next = listOf(first, disabled).withRuleEnabled("second", true)

        assertEquals(second, next[1])
    }

    @Test fun unknownOrUnchangedRuleKeepsTheSameInstances() {
        val rules = listOf(first, second)

        assertEquals(rules, rules.withRuleEnabled("missing", false))
        assertSame(first, rules.withRuleEnabled("first", true)[0])
    }
}
