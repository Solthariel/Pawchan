package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.MediaPreferences
import com.solthariel.pawchan.domain.settings.VideoCompletion

internal fun mediaBehaviorGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> {
    val media = app.behavior.client.media
    val change: ((MediaPreferences) -> MediaPreferences) -> Unit = { transform ->
        update { current -> current.withClient { it.copy(media = transform(it.media)) } }
    }
    return listOf(
        SettingsGroup(
            "Галерея и видео",
            listOf(
                SettingsItem.Toggle(
                    "Воспроизводить видео после перелистывания",
                    "При выключении следующее видео открывается на паузе",
                    media.playAfterScroll,
                ) { on ->
                    change {
                        it.copy(playAfterScroll = on)
                    }
                },
                SettingsItem.Toggle(
                    "Синхронизировать позицию треда с галереей",
                    "После закрытия просмотрщика перейти к посту с последним просмотренным файлом",
                    media.syncThreadPosition,
                ) { on ->
                    change {
                        it.copy(syncThreadPosition = on)
                    }
                },
                SettingsItem.Choice(
                    "После окончания видео",
                    listOf("Остановить", "Повторить"),
                    media.completion.ordinal,
                ) { index ->
                    change {
                        it.copy(completion = VideoCompletion.entries[index])
                    }
                },
                SettingsItem.Toggle(
                    "Предзагружать следующее видео",
                    "Заранее открывать соседнее видео; расходует трафик",
                    media.preloadVideo,
                ) { on ->
                    change {
                        it.copy(preloadVideo = on)
                    }
                },
            ),
        ),
    )
}
