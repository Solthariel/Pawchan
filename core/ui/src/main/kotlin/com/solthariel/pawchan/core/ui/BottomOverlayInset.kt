package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Высота плавающей панели навигации, которая лежит поверх содержимого экрана. Её задаёт владелец панели
 * (app), а список экрана добавляет её к нижнему отступу: последний элемент докручивается выше панели,
 * плашки и индикаторы не прячутся под ней. Ноль — панели нет или она скрыта (например, под клавиатурой).
 */
val LocalBottomOverlayInset = staticCompositionLocalOf { 0.dp }
