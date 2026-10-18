package com.solthariel.pawchan.domain.settings

import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.flow.Flow

/** Глобальные настройки из экрана «Настройки»; значения по умолчанию — поведение до появления настройки. */
data class AppSettings(
    val postMarkers: Boolean = true,
    val bubblePosts: Boolean = true,
    val hideStickers: Boolean = false,
    val showDefaultName: Boolean = false,
    val textSize: Int = DEFAULT_TEXT_SIZE,
    /** Длинные посты сворачиваются после стольких строк. */
    val collapseLines: Int = DEFAULT_COLLAPSE_LINES,
    val behavior: AppBehavior = AppBehavior(),
) {
    val postCountEnabled: Boolean get() = behavior.postCountEnabled
    val contentFilters: List<ContentFilterRule> get() = behavior.contentFilters

    fun withContentFilters(contentFilters: List<ContentFilterRule>): AppSettings =
        copy(behavior = behavior.copy(contentFilters = contentFilters))

    companion object {
        const val DEFAULT_TEXT_SIZE = 12
        const val MIN_TEXT_SIZE = 6
        const val MAX_TEXT_SIZE = 18
        const val DEFAULT_COLLAPSE_LINES = 20
        const val MIN_COLLAPSE_LINES = 10
        const val MAX_COLLAPSE_LINES = 30
    }
}

/** Поведение приложения: медиа, сохранённые копии, DNS Vocaroo и автоскрытие содержимого. */
data class AppBehavior(
    val postCountEnabled: Boolean = true,
    val videoStartMuted: Boolean = false,
    /** Сохранённые копии тредов дополняются новыми постами, пока тред жив. */
    val updateSavedCopies: Boolean = true,
    /** Через какой DNS искать серверы Vocaroo (только для записей Vocaroo). */
    val vocarooDns: VocarooDns = VocarooDns(),
    val contentFilters: List<ContentFilterRule> = emptyList(),
    val showHiddenThreads: Boolean = false,
    val client: ClientPreferences = ClientPreferences(),
)

/** Публичные DoH-серверы для Vocaroo; [SYSTEM] — обычный DNS телефона. */
enum class VocarooDnsProvider {
    SYSTEM,
    COMSS,
    XBOX,
    GEOHIDE,
    CUSTOM,
}

/** [customUrl] — свой адрес DoH (только https), используется при [VocarooDnsProvider.CUSTOM]. */
data class VocarooDns(
    val provider: VocarooDnsProvider = VocarooDnsProvider.SYSTEM,
    val customUrl: String = "",
)

interface AppSettingsRepository {
    val appSettings: Flow<AppSettings>

    suspend fun updateAppSettings(transform: (AppSettings) -> AppSettings): DomainOutcome<Unit>
}
