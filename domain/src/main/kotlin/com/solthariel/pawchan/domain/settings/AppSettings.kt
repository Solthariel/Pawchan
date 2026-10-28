package com.solthariel.pawchan.domain.settings

import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.flow.Flow

/** Глобальные настройки из экрана «Настройки»; значения по умолчанию — поведение до появления настройки. */
data class AppSettings(
    val postMarkers: Boolean = true,
    val bubblePosts: Boolean = false,
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
        copy(behavior = behavior.copy(autohide = behavior.autohide.copy(rules = contentFilters)))

    companion object {
        const val DEFAULT_TEXT_SIZE = 12
        const val MIN_TEXT_SIZE = 6
        const val MAX_TEXT_SIZE = 18
        const val DEFAULT_COLLAPSE_LINES = 20
        const val MIN_COLLAPSE_LINES = 10
        const val MAX_COLLAPSE_LINES = 30
    }
}

/** Поведение приложения: медиа, сохранённые копии, DNS сервисов и автоскрытие содержимого. */
data class AppBehavior(
    val postCountEnabled: Boolean = true,
    val videoStartMuted: Boolean = false,
    /** Сохранённые копии тредов дополняются новыми постами, пока тред жив. */
    val updateSavedCopies: Boolean = true,
    val dns: ServiceDnsPreferences = ServiceDnsPreferences(),
    val autohide: AutohidePreferences = AutohidePreferences(),
    val showHiddenThreads: Boolean = false,
    val client: ClientPreferences = ClientPreferences(),
) {
    val contentFilters: List<ContentFilterRule> get() = autohide.rules
    val contentFilterResetNotice: Boolean get() = autohide.resetNotice
}

data class AutohidePreferences(
    val rules: List<ContentFilterRule> = emptyList(),
    val resetNotice: Boolean = false,
)

/** Публичные DoH-серверы для внешних сервисов; [SYSTEM] — обычный DNS телефона. */
enum class DnsProvider {
    SYSTEM,
    COMSS,
    XBOX,
    GEOHIDE,
    CUSTOM,
}

/** [customUrl] — свой адрес DoH (только https), используется при [DnsProvider.CUSTOM]. */
data class ServiceDns(
    val provider: DnsProvider = DnsProvider.SYSTEM,
    val customUrl: String = "",
)

interface AppSettingsRepository {
    val appSettings: Flow<AppSettings>

    suspend fun updateAppSettings(transform: (AppSettings) -> AppSettings): DomainOutcome<Unit>
}

/** Независимые DNS-профили внешних сервисов, не меняющие DNS самого форума. */
data class ServiceDnsPreferences(
    val vocaroo: ServiceDns = ServiceDns(),
    val arhivach: ServiceDns = ServiceDns(),
    val github: ServiceDns = ServiceDns(),
)
