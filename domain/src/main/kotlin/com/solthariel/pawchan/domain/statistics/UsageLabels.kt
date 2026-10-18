package com.solthariel.pawchan.domain.statistics

fun UsageMetric.usageLabel(): String = requireNotNull(USAGE_LABELS[this])

private val USAGE_LABELS =
    mapOf(
        UsageMetric.THREADS_OPENED to "Открыто тредов",
        UsageMetric.POSTS_VIEWED to "Просмотрено постов",
        UsageMetric.POSTS_SENT to "Отправлено постов",
        UsageMetric.THREADS_CREATED to "Создано тредов",
        UsageMetric.REPLIES_SENT to "Отправлено ответов",
        UsageMetric.REPLIES_RECEIVED to "Получено ответов",
        UsageMetric.IMAGES_SENT to "Отправлено изображений",
        UsageMetric.VIDEOS_SENT to "Отправлено видео",
        UsageMetric.IMAGES_OPENED to "Открыто изображений",
        UsageMetric.VIDEOS_OPENED to "Открыто видео",
        UsageMetric.FILES_DOWNLOADED to "Скачано файлов",
        UsageMetric.BYTES_DOWNLOADED to "Объём скачанных файлов",
        UsageMetric.THREADS_FAVORITED to "Добавлено тредов в избранное",
        UsageMetric.THREADS_HIDDEN to "Скрыто тредов вручную",
        UsageMetric.THREADS_AUTO_HIDDEN to "Скрыто тредов автоматически",
        UsageMetric.STICKERS_HIDDEN to "Скрыто стикеров",
    )
