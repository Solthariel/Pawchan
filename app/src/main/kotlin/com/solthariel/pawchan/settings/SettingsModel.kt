package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.UiIconKind

/** Разделы главного списка настроек: значок, название и серое описание содержимого. */
internal enum class SettingsSection(
    val icon: UiIconKind,
    val title: String,
    val summary: String,
) {
    GENERAL(UiIconKind.TUNE, "Общие", "Чтение, история и блокировка приложения"),
    INTERFACE(UiIconKind.PALETTE, "Интерфейс", "Тема, размер текста, значки постов"),
    THREADS(UiIconKind.CHAT, "Треды и каталог", "Открытый тред, избранное, ответы и уведомления"),
    MEDIA(UiIconKind.IMAGE, "Медиа", "Звук видео, перелистывание, галерея"),
    POSTING(UiIconKind.TUNE, "Отправка", "Поля формы и обработка вложений по умолчанию"),
    AUTOHIDE(UiIconKind.FILTER, "Автоскрытие", "Правила скрытия постов и тредов"),
    NETWORK(UiIconKind.GLOBE, "Сеть", "DNS для Vocaroo и Архивача"),
    STORAGE(UiIconKind.TUNE, "Хранилище", "Лимит и очистка кэша, резервные копии, статистика"),
    ABOUT(UiIconKind.INFO, "О программе", "Версия и обновления"),
}

/** Строка настройки; [title] же участвует в поиске. */
internal sealed interface SettingsItem {
    val title: String

    data class Toggle(
        override val title: String,
        val summary: String?,
        val checked: Boolean,
        val onChange: (Boolean) -> Unit,
    ) : SettingsItem

    /** Выбор из списка: текущее значение серым, варианты — в общем диалоге выбора. */
    data class Choice(
        override val title: String,
        val options: List<String>,
        val selected: Int,
        val onSelect: (Int) -> Unit,
    ) : SettingsItem

    data class Scale(
        override val title: String,
        val value: Int,
        val range: IntRange,
        val unit: String,
        val preview: Boolean,
        val onChange: (Int) -> Unit,
    ) : SettingsItem

    /** Значение только для чтения или действие; [danger] — красное. */
    data class Action(
        override val title: String,
        val summary: String?,
        val danger: Boolean = false,
        val onClick: (() -> Unit)? = null,
    ) : SettingsItem

    data class Custom(
        override val title: String,
        val content: @Composable () -> Unit,
    ) : SettingsItem
}

internal data class SettingsGroup(
    val title: String,
    val items: List<SettingsItem>,
)

/** Поиск по названиям настроек во всех разделах. */
internal fun searchSettings(
    query: String,
    sections: Map<SettingsSection, List<SettingsGroup>>,
): List<SettingsGroup> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    return sections.mapNotNull { (section, groups) ->
        val found = groups.flatMap { it.items }.filter { it.title.contains(needle, ignoreCase = true) }
        found.takeIf { it.isNotEmpty() }?.let { SettingsGroup(section.title, it) }
    }
}
