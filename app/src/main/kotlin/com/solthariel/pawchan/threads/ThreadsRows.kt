package com.solthariel.pawchan.threads

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.UiDisclosureIndicator
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.modelLabel

/** Заголовок секции: нажатие сворачивает и разворачивает; справа сумма счётчиков и действие секции. */
@Composable
internal fun ThreadsSectionHeader(
    section: ThreadsSection,
    expanded: Boolean,
    total: ThreadCounter,
    onToggle: (() -> Unit)?,
    counterWidth: Dp? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(THREADS_HEADER_HEIGHT)
            .then(if (onToggle == null) Modifier else Modifier.clickable(onClick = onToggle))
            .padding(start = THREADS_ROW_START, end = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            section.title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val reply = section == ThreadsSection.REPLIES
        ThreadCounterColumn(total, reply, counterWidth)
        Box(Modifier.size(THREADS_TRAILING_WIDTH), contentAlignment = Alignment.Center) {
            if (onToggle != null) UiDisclosureIndicator(expanded)
        }
    }
}

/** Тред одной строкой: сайт, название и счётчик; долгое нажатие — меню. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ThreadRow(
    row: ThreadRowUi,
    current: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    closeAction: ThreadRowCloseAction? = null,
    counterWidth: Dp? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(THREADS_ROW_HEIGHT)
            .background(if (current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = THREADS_ROW_START, end = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreadSourceIcon(row.source, current)
        Text(
            row.title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color =
                if (current) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        )
        ThreadCounterColumn(row.counter, row.reply, counterWidth)
        Box(Modifier.size(THREADS_TRAILING_WIDTH), contentAlignment = Alignment.Center) {
            closeAction?.let { action ->
                CatalogIconButton(UiIconKind.CLOSE, action.label, onClick = action.onClick)
            }
        }
    }
}

/**
 * Правая колонка шириной с кнопку «Закрыть»: счётчик стоит по её центру, поэтому
 * в строках, заголовках и рядом с крестиками соседних секций он на одной вертикали.
 * Длинное число расширяет колонку сразу во всех строках.
 */
@Composable
private fun ThreadCounterColumn(
    counter: ThreadCounter,
    reply: Boolean,
    width: Dp?,
) {
    val cell = width ?: COUNTER_MIN_WIDTH
    Box(
        Modifier.width(maxOf(THREADS_TRAILING_WIDTH, cell)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(Modifier.width(THREADS_TRAILING_WIDTH), contentAlignment = Alignment.Center) {
            ThreadCounterCell(counter, reply, cell)
        }
    }
}

/**
 * Постоянная ячейка счётчика строки: скруглённая плашка, ноль приглушён,
 * ненулевое число — акцентным цветом (ответы вам — цветом ошибки, как бейдж «Тредов»).
 * Ширина общая для всех строк экрана и растёт по самому длинному числу.
 */
@Composable
internal fun ThreadCounterCell(
    counter: ThreadCounter,
    reply: Boolean,
    width: Dp,
) {
    val colors = MaterialTheme.colorScheme
    val active = !counter.deleted && (counter.count > 0 || counter.error)
    val container =
        when {
            !active -> colors.surfaceVariant
            reply -> colors.error
            else -> colors.primary
        }
    val content =
        when {
            !active -> colors.onSurfaceVariant.copy(alpha = MUTED_ZERO_ALPHA)
            reply -> colors.onError
            else -> colors.onPrimary
        }
    Box(
        Modifier
            .height(COUNTER_HEIGHT)
            .requiredWidth(width)
            .background(container, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val label = counter.cellLabel
        if (label.isEmpty()) {
            Box(Modifier.size(6.dp).background(content, CircleShape))
        } else {
            Text(label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
        }
    }
}

/** Ширина ячейки по самому длинному числу среди строк: 0 → 10 → 100 расширяют все ячейки сразу. */
@Composable
internal fun rememberThreadCounterWidth(sections: List<List<ThreadRowUi>>): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    // Сумма в заголовке секции стоит в той же колонке, поэтому тоже участвует в ширине.
    val totals =
        sections.map { rows ->
            maxOf(
                ThreadCounter(rows.sumOf { it.counter.count }).cellLabel.length,
                ThreadCounter(rows.size).cellLabel.length,
            )
        }
    val widest = (sections.flatten().map { it.counter.cellLabel.length } + totals).maxOrNull() ?: 1
    return remember(widest, style, density) {
        val text = measurer.measure("0".repeat(widest.coerceAtLeast(1)), style, maxLines = 1)
        val measured = with(density) { text.size.width.toDp() } + COUNTER_HORIZONTAL_PADDING * 2
        measured.coerceAtLeast(COUNTER_HEIGHT)
    }
}

/** Пустая секция. */
@Composable
internal fun ThreadsEmptyRow(text: String) {
    Text(
        modelLabel(text),
        Modifier.fillMaxWidth().padding(horizontal = THREADS_ROW_START, vertical = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Переход на отдельный экран: История, Сохранённые копии. */
@Composable
internal fun ThreadsLinkRow(
    icon: UiIconKind,
    title: String,
    count: Int?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(THREADS_ROW_HEIGHT)
            .clickable(onClick = onClick)
            .padding(start = THREADS_ROW_START, end = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.solthariel.pawchan.core.ui
            .UiIcon(icon, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(24.dp))
        Text(
            title,
            Modifier.weight(1f).padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (count != null && count > 0) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(Modifier.width(THREADS_TRAILING_WIDTH), contentAlignment = Alignment.Center) {
            UiForwardIndicator()
        }
    }
}

private val THREADS_ROW_HEIGHT = 48.dp
private val THREADS_HEADER_HEIGHT = 48.dp
private val THREADS_ROW_START = 16.dp
private val COUNTER_HEIGHT = 26.dp
private val COUNTER_MIN_WIDTH = 26.dp
private val COUNTER_HORIZONTAL_PADDING = 6.dp
private const val MUTED_ZERO_ALPHA = 0.6f

/** Общая крайняя колонка стрелок заголовков и крестиков строк. */
private val THREADS_TRAILING_WIDTH = APP_TOOLBAR_BUTTON_SIZE
