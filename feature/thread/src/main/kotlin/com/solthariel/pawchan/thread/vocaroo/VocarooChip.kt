package com.solthariel.pawchan.thread.vocaroo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.thread.LocalPostLinkAddress
import com.solthariel.pawchan.thread.LocalPostLinks
import com.solthariel.pawchan.thread.PostLinkMenu
import com.solthariel.pawchan.thread.PostLinkTarget

/**
 * Плашка на месте ссылки Vocaroo: нажатие — играть/пауза, полоса — перемотка,
 * долгое нажатие — меню ссылки (копировать, открыть в браузере, скачать mp3).
 */
@Composable
internal fun VocarooChip(url: String) {
    val player = LocalVocarooPlayer.current ?: return
    val id = remember(url) { vocarooId(url) } ?: return
    val state = player.state
    val active = state.id == id
    var menu by remember(url) { mutableStateOf<PostLinkTarget?>(null) }
    val resolver = LocalPostLinks.current?.resolver
    val address = LocalPostLinkAddress.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(percent = 50))
            .background(accent.copy(alpha = CHIP_BACKGROUND_ALPHA))
            .semantics { contentDescription = "Голосовое Vocaroo" }
            .pointerInput(id) {
                detectTapGestures(
                    onTap = { player.toggle(id) },
                    onLongPress = {
                        val resolved = RemoteUrl.parse(url)?.let { resolver?.resolve(PostLink.External(it), address) }
                        menu = vocarooLinkTarget(url, id, resolved)
                    },
                )
            }.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VocarooChipIcon(active, state)
        Spacer(Modifier.width(4.dp))
        if (active && state.duration > 0 && !state.failed) {
            VocarooSeekBar(state.position.toFloat() / state.duration, Modifier.weight(1f)) { player.seek(id, it) }
            Spacer(Modifier.width(6.dp))
            VocarooLabel("${formatVocarooTime(state.position)} / ${formatVocarooTime(state.duration)}")
        } else {
            VocarooLabel(if (active) activeLabel(state, player.duration(id)) else idleLabel(player.duration(id)))
        }
    }
    PostLinkMenu(menu) { menu = null }
}

private fun idleLabel(duration: Int?): String =
    if (duration != null && duration > 0) "Vocaroo · ${formatVocarooTime(duration)}" else "Vocaroo"

private fun activeLabel(
    state: VocarooState,
    duration: Int?,
): String =
    when {
        state.failed -> "Vocaroo · ошибка, повторить"
        state.preparing -> state.progress?.let { "Vocaroo · ${(it * PERCENT).toInt()}%" } ?: "Vocaroo · загрузка"
        else -> idleLabel(duration)
    }

@Composable
private fun VocarooChipIcon(
    active: Boolean,
    state: VocarooState,
) {
    val accent = MaterialTheme.colorScheme.primary
    if (active && state.preparing) {
        CircularProgressIndicator(
            Modifier.fillMaxHeight(ICON_FRACTION).aspectRatio(1f),
            color = accent,
            strokeWidth = 2.dp,
        )
    } else {
        val kind = if (active && state.playing) UiIconKind.PAUSE else UiIconKind.PLAY
        UiIcon(kind, accent, Modifier.fillMaxHeight(ICON_FRACTION).aspectRatio(1f))
    }
}

@Composable
private fun VocarooLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium.copy(lineHeight = 1.em),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun VocarooSeekBar(
    progress: Float,
    modifier: Modifier = Modifier,
    onSeek: (Float) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val track = accent.copy(alpha = TRACK_ALPHA)
    Box(
        modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures { onSeek(it.x / size.width) }
            }.pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSeek(change.position.x / size.width)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2
            val stroke = 3.dp.toPx()
            val x = size.width * progress.coerceIn(0f, 1f)
            drawLine(track, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
            drawLine(accent, Offset(0f, y), Offset(x, y), stroke, StrokeCap.Round)
            drawCircle(accent, KNOB_RADIUS.dp.toPx(), Offset(x, y))
        }
    }
}

/** Меню ссылки как у обычных ссылок, но «Скачать файл» качает сам mp3. */
private fun vocarooLinkTarget(
    url: String,
    id: String,
    resolved: PostLinkTarget?,
): PostLinkTarget {
    val mp3 = vocarooMp3Url(id)
    val source = MediaAttachmentSource(mp3, "Vocaroo $id.mp3", mp3, null)
    val attachment = MediaAttachmentUi(source, null, AttachmentViewer.NONE)
    return (resolved ?: PostLinkTarget(url)).copy(attachment = attachment)
}

private const val ICON_FRACTION = 0.8f
private const val PERCENT = 100
private const val CHIP_BACKGROUND_ALPHA = 0.14f
private const val TRACK_ALPHA = 0.3f
private const val KNOB_RADIUS = 4
