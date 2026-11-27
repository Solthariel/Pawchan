package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources

/** Presentation only: never apply to post bodies, subjects, file names or editor input. */
@Composable
fun modelLabel(text: String): String {
    val resources = LocalResources.current
    val id = modelLabels[text]
    if (id != null) return resources.getString(id)
    val format = modelFormats.firstOrNull { it.matches(text) }
    return if (format == null) text else resources.getString(format.id, format.argument(text))
}

private data class ModelFormat(
    val prefix: String,
    val suffix: String,
    val id: Int,
) {
    fun matches(text: String): Boolean {
        val longEnough = text.length >= prefix.length + suffix.length
        return longEnough && text.startsWith(prefix) && text.endsWith(suffix)
    }

    fun argument(text: String): String = text.substring(prefix.length, text.length - suffix.length)
}

private val modelLabels by lazy {
    listOf(
        modelLabels0,
        modelLabels1,
        modelLabels2,
        modelLabels3,
        modelLabels4,
        modelLabels5,
        modelLabels6,
        modelLabels7,
    ).fold(emptyMap<String, Int>()) { acc, next ->
        acc +
            next
    }
}
private val modelFormats =
    listOf(
        ModelFormat("Сервер ограничил запросы. Повторить через ", " с", R.string.msg_160c3b67d6),
        ModelFormat("Восстановлено удалённых постов: ", "", R.string.msg_5842bd591f),
        ModelFormat("Проверено с этого устройства в ", "", R.string.msg_fa380ca6f2),
        ModelFormat("Медиа сохранены не полностью: ", "", R.string.msg_5235175e94),
        ModelFormat("Новых ответов на ваши посты: ", "", R.string.msg_7b7f4280e3),
        ModelFormat("Не удалось обновить копий: ", "", R.string.msg_b2ffb05eab),
        ModelFormat("Ошибка чтения или записи: ", ". ", R.string.msg_f9b559bd6c),
        ModelFormat("Архивач ответил ошибкой ", "", R.string.msg_acf3c6e6bb),
        ModelFormat("Неизвестная настройка: ", "", R.string.msg_3334b9b7a4),
        ModelFormat("Обновление не удалось: ", ". Локальная копия сохранена.", R.string.msg_a711c91c7a),
        ModelFormat("Сейчас используется: ", "", R.string.msg_18dacb4f1b),
        ModelFormat("Ошибка оценки: HTTP ", "", R.string.msg_a920b1af56),
        ModelFormat("Повторяющееся поле: ", "", R.string.msg_08a1197ba8),
        ModelFormat("Версия приложения: ", "", R.string.msg_c4143a718c),
        ModelFormat("Сохранено правил: ", "", R.string.msg_66af3d5eac),
        ModelFormat("Добавлено правил: ", ". Текущие правила сохранены, повторы пропущены.", R.string.msg_0313939d75),
        ModelFormat("Статус Архивача: ", "", R.string.msg_ded8ac714c),
        ModelFormat("HTTP-проверка: ", " мс", R.string.msg_69735ccc1e),
        ModelFormat("Очистить кэш — ", "", R.string.msg_09176fd3e4),
        ModelFormat("Дата в архиве: ", "", R.string.msg_f8abb2ed13),
        ModelFormat("Скрытый тред #", "", R.string.msg_68b8734150),
        ModelFormat("Автоскрытие: ", "", R.string.msg_ffe4ca0af6),
        ModelFormat("Медиа: HTTP ", "", R.string.msg_758bc2966d),
        ModelFormat(" • новых: ", "", R.string.msg_db56843188),
        ModelFormat("Ответ за ", " мс", R.string.msg_9de17a9c92),
        ModelFormat("Среднее: ", " мс", R.string.msg_0236129b46),
        ModelFormat("Отправка ", "%", R.string.msg_f771223a80),
        ModelFormat("Файлов: ", "", R.string.msg_189f43f175),
        ModelFormat("Постов: ", "", R.string.msg_95e989be97),
        ModelFormat("Тред №", "", R.string.msg_751f719962),
    )
