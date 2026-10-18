package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot

/** Сохранение треда: локальная копия (диалог приложения) и Архивач; null — Архивача нет. */
data class ThreadArchiveHost(
    val copyDialog: @Composable (ThreadAddress?, () -> Unit) -> Unit,
    val external: ThreadExternalArchive? = null,
)

/** Архивач: ссылка на копию треда, доступность сайта, капча при добавлении и загрузка копии. */
data class ThreadExternalArchive(
    val state: @Composable (ThreadAddress) -> ThreadExternalArchiveState,
    val submitDialog: @Composable (ThreadAddress, () -> Unit) -> Unit,
    val open: (String) -> Unit,
    val load: suspend (ThreadAddress, String) -> DomainOutcome<ThreadSnapshot>,
)

data class ThreadExternalArchiveState(
    /** Копия на Архиваче, добавленная из приложения; null — тред туда не отправлялся. */
    val link: String? = null,
    /** false — сайт не открылся при последней проверке: пункты меню серые. */
    val available: Boolean = true,
)

internal val LocalThreadArchive = staticCompositionLocalOf<ThreadArchiveHost?> { null }
