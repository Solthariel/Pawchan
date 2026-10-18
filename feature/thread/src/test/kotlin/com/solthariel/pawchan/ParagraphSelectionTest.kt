package com.solthariel.pawchan

import androidx.compose.ui.text.TextRange
import com.solthariel.pawchan.thread.paragraphRange
import org.junit.Assert.assertEquals
import org.junit.Test

class ParagraphSelectionTest {
    @Test fun doubleTapRangeIsParagraphNotWord() {
        assertEquals(TextRange(0, 17), paragraphRange("one two three end\nnext", 5))
    }

    @Test fun secondParagraphDoesNotIncludePreviousLine() {
        assertEquals(TextRange(4, 13), paragraphRange("one\ntwo three", 7))
    }

    @Test fun emptyTextAndEndOffsetAreSafe() {
        assertEquals(TextRange.Zero, paragraphRange("", 10))
        assertEquals(TextRange(0, 3), paragraphRange("one", 99))
    }
}
