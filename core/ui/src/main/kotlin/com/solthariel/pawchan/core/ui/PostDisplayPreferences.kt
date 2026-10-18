package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Глобальные настройки вида поста; приложение кладёт их в [LocalPostDisplayPreferences]. */
data class PostDisplayPreferences(
    /** Часовой пояс телефона; передаётся явно для детерминированного форматирования. */
    val zone: ZoneId = ZoneId.systemDefault(),
    val showStateMarkers: Boolean = true,
    val bubblePosts: Boolean = true,
    val hideStickers: Boolean = false,
    /** Показывать имя по умолчанию («Аноним»). */
    val showDefaultName: Boolean = false,
    /** Размер текста и порог сворачивания для вновь открытых тредов (в треде их можно поменять). */
    val textSize: Int = DEFAULT_POST_TEXT_SIZE,
    val collapseLines: Int = DEFAULT_POST_COLLAPSE_LINES,
)

const val DEFAULT_POST_TEXT_SIZE = 12
const val DEFAULT_POST_COLLAPSE_LINES = 20

val LocalPostDisplayPreferences = staticCompositionLocalOf { PostDisplayPreferences() }

/** Local post date, including the year for every post. */
fun formatPostTime(
    timestampSeconds: Long,
    preferences: PostDisplayPreferences,
): String = FULL_DATE.format(Instant.ofEpochSecond(timestampSeconds).atZone(preferences.zone))

private val FULL_DATE = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss")
