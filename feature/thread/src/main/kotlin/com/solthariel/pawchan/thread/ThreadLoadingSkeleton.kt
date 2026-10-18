package com.solthariel.pawchan.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Первая загрузка треда из сети: вместо пустого фона со спиннером — каркас постов в цветах темы.
 * Ход загрузки показывает индикатор обновления сверху; каркас неподвижен и не анимируется.
 * Повторный вход в удержанный тред сюда не попадает: он сразу показывает посты.
 */
@Composable
internal fun ThreadLoadingSkeleton() {
    val shade = MaterialTheme.colorScheme.onSurface.copy(alpha = SKELETON_ALPHA)
    val divider = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxSize()) {
        SKELETON_POSTS.forEach { post ->
            SkeletonPost(post, shade)
            HorizontalDivider(color = divider)
        }
    }
}

private class SkeletonShape(
    val withImage: Boolean,
    val lines: List<Float>,
)

@Composable
private fun SkeletonPost(
    shape: SkeletonShape,
    shade: Color,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        SkeletonBar(HEADER_FRACTION, shade)
        Spacer(Modifier.height(10.dp))
        Row {
            if (shape.withImage) {
                Box(Modifier.size(IMAGE_SIZE).background(shade, RoundedCornerShape(4.dp)))
                Spacer(Modifier.size(10.dp))
            }
            Column(Modifier.weight(1f)) {
                shape.lines.forEach { fraction ->
                    SkeletonBar(fraction, shade)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SkeletonBar(
    fraction: Float,
    shade: Color,
) {
    Box(Modifier.fillMaxWidth(fraction).height(LINE_HEIGHT).background(shade, RoundedCornerShape(3.dp)))
}

/** Неравномерные, но постоянные посты: через один с картинкой, от одной до четырёх строк, последняя короче. */
private val SKELETON_POSTS =
    List(SKELETON_POST_COUNT) { index ->
        val lines = 1 + (index * SKELETON_LINE_STEP) % SKELETON_MAX_LINES
        SkeletonShape(
            withImage = index % 2 == 0,
            lines = List(lines) { line -> if (line == lines - 1) SKELETON_LAST_LINE else 1f },
        )
    }

private const val SKELETON_POST_COUNT = 6
private const val SKELETON_MAX_LINES = 4
private const val SKELETON_LINE_STEP = 3
private const val SKELETON_LAST_LINE = 0.6f
private const val SKELETON_ALPHA = 0.08f
private const val HEADER_FRACTION = 0.4f
private val LINE_HEIGHT = 10.dp
private val IMAGE_SIZE = 72.dp
