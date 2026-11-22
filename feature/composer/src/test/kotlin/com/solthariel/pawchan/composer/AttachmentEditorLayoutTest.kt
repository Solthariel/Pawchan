package com.solthariel.pawchan.composer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AttachmentEditorLayoutTest {
    @Test fun narrowWidthsUseMenusWithoutShrinkingTargets() {
        assertEquals(HeaderLayout.COMPACT, attachmentHeaderLayout(180f, 1f))
        assertEquals(HeaderLayout.WITH_MENU, attachmentHeaderLayout(232f, 1f))
        assertEquals(HeaderLayout.DIRECT, attachmentHeaderLayout(360f, 1f))
        assertEquals(HeaderLayout.DIRECT, attachmentHeaderLayout(360f, 2f))
    }

    @Test fun textEditingTabIsNotPresented() {
        assertEquals(
            listOf("Файл", "Имя", "Пропорции", "Поворот", "Рисовать", "Замазать"),
            AttachmentEditorTab.entries.map { it.label },
        )
        assertFalse(AttachmentEditorTab.entries.any { it.label == "Текст" })
    }
}
