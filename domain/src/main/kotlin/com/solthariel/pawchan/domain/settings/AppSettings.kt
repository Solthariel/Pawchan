package com.solthariel.pawchan.domain.settings

import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Глобальные настройки из экрана «Настройки»; значения по умолчанию — поведение до появления настройки. */
data class AppSettings(
    val postMarkers: Boolean = true,
    val hideStickers: Boolean = false,
    val showDefaultName: Boolean = false,
    val textSize: Int = DEFAULT_TEXT_SIZE,
    /** Длинный OP сворачивается после стольких строк; 31 означает «не сворачивать». */
    val collapseLines: Int = DEFAULT_COLLAPSE_LINES,
    val behavior: AppBehavior = AppBehavior(),
    val hideMonkeyReplies: Boolean = false,
) {
    val postCountEnabled: Boolean get() = behavior.postCountEnabled
    val contentFilters: List<ContentFilterRule> get() = behavior.contentFilters

    fun withContentFilters(contentFilters: List<ContentFilterRule>): AppSettings =
        copy(behavior = behavior.copy(autohide = behavior.autohide.copy(rules = contentFilters)))

    companion object {
        const val DEFAULT_TEXT_SIZE = 12
        const val MIN_TEXT_SIZE = 6
        const val MAX_TEXT_SIZE = 18
        const val DEFAULT_COLLAPSE_LINES = 20
        const val MIN_COLLAPSE_LINES = 10
        const val NO_COLLAPSE = 31
    }
}

/** Поведение приложения: медиа, сохранённые копии и автоскрытие содержимого. */
data class AppBehavior(
    val postCountEnabled: Boolean = true,
    val videoStartMuted: Boolean = false,
    /** Сохранённые копии тредов дополняются новыми постами, пока тред жив. */
    val updateSavedCopies: Boolean = true,
    val autohide: AutohidePreferences = AutohidePreferences(),
    val showHiddenThreads: Boolean = false,
    val client: ClientPreferences = ClientPreferences(),
    val showHiddenPosts: Boolean = DEFAULT_SHOW_HIDDEN_POSTS,
) {
    val contentFilters: List<ContentFilterRule> get() = autohide.rules
    val contentFilterResetNotice: Boolean get() = autohide.resetNotice
}

data class AutohidePreferences(
    val rules: List<ContentFilterRule> = emptyList(),
    val resetNotice: Boolean = false,
)

interface AppSettingsRepository {
    val appSettings: Flow<AppSettings>

    suspend fun updateAppSettings(transform: (AppSettings) -> AppSettings): DomainOutcome<Unit>
}

/** Previous versions always showed collapsed rows for replies hidden by a content filter. */
const val DEFAULT_SHOW_HIDDEN_POSTS = true

fun AppSettingsRepository?.hiddenPostPlaceholders(): Flow<Boolean> =
    this?.appSettings?.map { it.behavior.showHiddenPosts }?.distinctUntilChanged() ?: flowOf(DEFAULT_SHOW_HIDDEN_POSTS)
