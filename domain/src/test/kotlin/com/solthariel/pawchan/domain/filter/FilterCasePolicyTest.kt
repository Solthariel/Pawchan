package com.solthariel.pawchan.domain.filter

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FilterCasePolicyTest {
    @Test fun latinAndCyrillicAlwaysIgnoreCaseIncludingPreviouslySensitiveRules() {
        listOf(false, true).forEach { savedCase ->
            val rule = builderRule(ContentFilterMode.ANY_TERM, listOf("VIP", "кот"), sensitive = savedCase)
            val engine = ContentFilterEngine(listOf(rule))
            listOf("VIP", "vip", "Vip", "Кот", "КОТ", "кот").forEach {
                assertNotNull(it, engine.comment(builderBoard, it))
            }
            assertNull(engine.comment(builderBoard, "VIP_1"))
            assertNull(engine.comment(builderBoard, "котлета"))
        }
    }

    @Test fun exceptionsIgnoreCaseAsWellAndKeepTheWholePostVisible() {
        val rule = builderRule(terms = listOf("кот"), sensitive = true, exceptions = listOf("ПРИЮТ"))
        val engine = ContentFilterEngine(listOf(rule))
        assertNull(engine.comment(builderBoard, "КОТ приют"))
        assertNull(engine.threadMatch(builderBoard, "Кот", "приют"))
        assertNotNull(engine.comment(builderBoard, "КОТ приютский"))
    }
}
