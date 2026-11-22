package com.solthariel.pawchan.boards

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardCardSurfaceTest {
    @Test fun pureBlackCardsRemainVisibleWithoutChangingOtherThemes() {
        assertEquals(Color(0xff161616), boardCardSurface(Color.Black, Color.Black))
        assertEquals(Color.DarkGray, boardCardSurface(Color.Black, Color.DarkGray))
        assertEquals(Color.White, boardCardSurface(Color.White, Color.White))
    }
}
