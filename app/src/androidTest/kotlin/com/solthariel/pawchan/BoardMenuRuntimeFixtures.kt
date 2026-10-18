package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.BoardInformationState
import com.solthariel.pawchan.catalog.BoardMenuNavigation
import com.solthariel.pawchan.catalog.BoardMenuPreferences
import com.solthariel.pawchan.catalog.BoardPreferenceActions
import com.solthariel.pawchan.catalog.CatalogMenuBindings
import com.solthariel.pawchan.catalog.CatalogMenuState

internal fun testBoardMenu(
    preferences: BoardMenuPreferences = BoardMenuPreferences(),
    navigation: BoardMenuNavigation = BoardMenuNavigation({}, {}, {}),
    actions: BoardPreferenceActions = BoardPreferenceActions({}, {}, {}),
): CatalogMenuBindings =
    CatalogMenuBindings(
        CatalogMenuState(preferences, BoardInformationState.Closed),
        actions,
        navigation,
        {},
    )
