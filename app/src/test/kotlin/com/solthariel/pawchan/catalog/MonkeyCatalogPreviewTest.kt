package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostTextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonkeyCatalogPreviewTest {
    @Test fun authorOnlyAndLabeledCompactVariantsArePreparedOnce() {
        val runs = listOf(PostTextRun("@monkey вопрос\n"), PostTextRun("ответ\n", monkeyBlock = 1))
        val thread = PostDocument(runs).toCatalogText()
        assertEquals("@monkey вопрос\nОтвет @monkey: ответ", thread.preview)
        assertEquals("@monkey вопрос", thread.authorPreview)
        assertNull(PostDocument.plain("@monkey обычное упоминание").toCatalogText().authorPreview)
    }
}
