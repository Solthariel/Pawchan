package com.solthariel.pawchan

import com.solthariel.pawchan.ui.FavoriteMenuAction
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteMenuContractTest {
    @Test fun onlyThreeActionsAreAvailableInTheExpectedOrder() {
        assertEquals(
            listOf("Открыть тред", "Копировать ссылку", "Убрать тред из избранного"),
            FavoriteMenuAction.entries.map { it.label },
        )
    }
}
