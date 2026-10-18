package com.solthariel.pawchan.domain.posting

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostingRulesTest {
    @Test
    fun checksFileCountSizeAndExtensionSeparatelyFromGridLayout() {
        val rules = PostingRules(maxFiles = 4, maxFileBytes = 3)
        val file = ReplyAttachment("cat.PNG", "image/png", byteArrayOf(1, 2, 3))
        assertTrue(rules.accepts(List(4) { file }))
        assertFalse(rules.accepts(List(5) { file }))
        assertFalse(rules.accepts(listOf(file.copy(content = byteArrayOf(1, 2, 3, 4)))))
        assertFalse(rules.accepts(listOf(file.copy(fileName = "cat.exe"))))
        assertTrue(PostingRules(maxFiles = 0).accepts(emptyList()))
        assertFalse(PostingRules(maxFiles = 0).accepts(listOf(file)))
    }
}
