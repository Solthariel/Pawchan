package com.solthariel.pawchan.media.thumbnail

import androidx.compose.runtime.staticCompositionLocalOf

/** Загрузчик миниатюр списка: просмотрщик сразу показывает уже декодированную миниатюру из его памяти. */
val LocalThumbnailLoader = staticCompositionLocalOf<ThumbnailLoader?> { null }
