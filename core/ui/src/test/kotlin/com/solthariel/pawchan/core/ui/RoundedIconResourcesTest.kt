package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundedIconResourcesTest {
    @Test fun requestedSettingsSymbolsDoNotReuseLanguageOrFilter() {
        assertEquals(R.drawable.ic_rounded_public, roundedIconResource(UiIconKind.PUBLIC))
        assertEquals(R.drawable.ic_rounded_cleaning_services, roundedIconResource(UiIconKind.CLEANING))
        assertNotEquals(roundedIconResource(UiIconKind.GLOBE), roundedIconResource(UiIconKind.PUBLIC))
        assertNotEquals(roundedIconResource(UiIconKind.FILTER), roundedIconResource(UiIconKind.CLEANING))
    }

    @Test fun everyStandardActionHasABundledResource() {
        UiIconKind.entries.forEach {
            assertTrue(it.name, roundedIconResource(it) != 0)
        }
    }

    @Test fun sourceMarkersUseOfficialRoundedSymbols() {
        assertEquals(R.drawable.ic_rounded_person, roundedIconResource(UiIconKind.PERSON))
        assertEquals(R.drawable.ic_rounded_arrow_downward, roundedIconResource(UiIconKind.SAGE))
        assertEquals(R.drawable.ic_rounded_autorenew, roundedIconResource(UiIconKind.INFINITY))
    }

    @Test fun bookmarksDistinguishAddFromSelected() {
        assertEquals(R.drawable.ic_rounded_bookmark_add, roundedIconResource(UiIconKind.BOOKMARK_ADD))
        assertEquals(R.drawable.ic_rounded_bookmark_fill, roundedIconResource(UiIconKind.BOOKMARK_FILLED))
        assertNotEquals(roundedIconResource(UiIconKind.BOOKMARK_ADD), roundedIconResource(UiIconKind.BOOKMARK_FILLED))
    }

    @Test fun boardStarsHaveDistinctSelectionGeometry() {
        assertEquals(R.drawable.ic_rounded_star, roundedIconResource(UiIconKind.STAR))
        assertEquals(R.drawable.ic_rounded_star_fill, roundedIconResource(UiIconKind.STAR_FILLED))
        assertNotEquals(roundedIconResource(UiIconKind.STAR), roundedIconResource(UiIconKind.STAR_FILLED))
    }

    @Test fun galleryUsesPhotoLibraryNotFullscreenOrSingleImage() {
        val gallery = roundedIconResource(UiIconKind.MEDIA_MULTIPLE)
        assertEquals(R.drawable.ic_rounded_photo_library, gallery)
        assertNotEquals(roundedIconResource(UiIconKind.EXPAND), gallery)
        assertNotEquals(roundedIconResource(UiIconKind.IMAGE), gallery)
        assertNotEquals(roundedIconResource(UiIconKind.GRID), gallery)
    }

    @Test fun basicActionsKeepTheirMeaning() {
        assertEquals(R.drawable.ic_rounded_search, roundedIconResource(UiIconKind.SEARCH))
        assertEquals(R.drawable.ic_rounded_refresh, roundedIconResource(UiIconKind.REFRESH))
        assertEquals(R.drawable.ic_rounded_settings, roundedIconResource(UiIconKind.SETTINGS))
        assertEquals(R.drawable.ic_rounded_bookmark, roundedIconResource(UiIconKind.BOOKMARK))
        assertEquals(R.drawable.ic_rounded_download, roundedIconResource(UiIconKind.SAVE))
    }

    @Test fun ratingDirectionAndSelectionHaveFourDistinctResources() {
        val states = listOf(true to true, true to false, false to true, false to false)
        assertEquals(4, states.map { (up, filled) -> ratingIconResource(up, filled) }.toSet().size)
    }

    @Test fun selectedRatingIsFilledWithoutChangingDirection() {
        assertEquals(R.drawable.ic_rounded_thumb_up_fill, ratingIconResource(up = true, filled = true))
        assertEquals(R.drawable.ic_rounded_thumb_up, ratingIconResource(up = true, filled = false))
        assertEquals(R.drawable.ic_rounded_thumb_down_fill, ratingIconResource(up = false, filled = true))
        assertEquals(R.drawable.ic_rounded_thumb_down, ratingIconResource(up = false, filled = false))
    }
}
