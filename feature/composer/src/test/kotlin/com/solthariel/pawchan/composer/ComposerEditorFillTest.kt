package com.solthariel.pawchan.composer

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerEditorFillTest {
    @Test
    fun fieldFillsTheFreeHeightOnlyWhenTheMinimumFits() {
        assertTrue(composerEditorFills(COMPOSER_EDITOR_FILL_HEIGHT))
        assertTrue(composerEditorFills(600.dp))
        assertFalse(composerEditorFills(COMPOSER_EDITOR_FILL_HEIGHT - 1.dp))
    }
}
