package com.solthariel.pawchan.navigation.chrome

import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.settings.BottomBarButton
import com.solthariel.pawchan.domain.settings.NavigationPreferences

/** App-owned navigation appearance; post rendering and icon alpha are independent. */
internal val LocalBottomBarTransparency =
    staticCompositionLocalOf {
        NavigationPreferences.DEFAULT_BOTTOM_BAR_TRANSPARENCY
    }

/** Порядок кнопок нижней панели слева направо из «Интерфейс → Нижняя панель». */
internal val LocalBottomBarOrder =
    staticCompositionLocalOf<List<BottomBarButton>> { BottomBarButton.entries }

internal fun bottomBarSurfaceAlpha(transparencyPercent: Int): Float =
    1f - transparencyPercent.coerceIn(0, NavigationPreferences.MAX_TRANSPARENCY_PERCENT) /
        NavigationPreferences.MAX_TRANSPARENCY_PERCENT.toFloat()
