package com.solthariel.pawchan.thread

import java.io.IOException

/** A bounded, ordered set of actual pixel strips. A missing or incompatible strip is never silently omitted. */
internal class ShotSelection<T>(
    private val budget: Long = THREAD_SHOT_MAX_PIXELS,
) {
    private val rows = mutableMapOf<Long, ShotRow<T>>()

    fun select(
        id: Long,
        geometry: ShotGeometry,
    ) {
        val pixels = geometry.pixels
        val used = rows.values.sumOf { it.geometry.pixels } - (rows[id]?.geometry?.pixels ?: 0L)
        if (pixels <= 0 || pixels > budget - used) throw IOException(SHOT_TOO_LARGE)
        rows[id] = ShotRow(geometry)
    }

    fun remove(id: Long) {
        rows.remove(id)
    }

    fun clear() = rows.clear()

    fun store(
        id: Long,
        geometry: ShotGeometry,
        pixels: T,
    ) {
        val row = rows[id]
        if (row?.geometry == geometry) row.image = pixels
    }

    fun isReady(selected: Set<Long>): Boolean = selected.isNotEmpty() && selected.all { rows[it]?.image != null }

    fun snapshot(
        selected: Set<Long>,
        displayOrder: List<Long>,
    ): List<T> {
        val ids = displayOrder.filter(selected::contains)
        if (!isReady(selected) || ids.size != selected.size || ids.toSet() != selected) throw IOException(SHOT_MISSING)
        val strips = ids.map { rows.getValue(it) }
        if (strips.any { it.geometry.width != strips.first().geometry.width }) throw IOException(SHOT_WIDTH_CHANGED)
        return strips.map { checkNotNull(it.image) }
    }
}

internal data class ShotGeometry(
    val width: Int,
    val height: Int,
) {
    val pixels: Long get() = if (width > 0 && height > 0) width.toLong() * height else 0L
}

private data class ShotRow<T>(
    val geometry: ShotGeometry,
    var image: T? = null,
)

internal const val THREAD_SHOT_MAX_PIXELS = 8_000_000L
internal const val SHOT_TOO_LARGE = "Слишком большой скриншот: выберите меньше постов"
internal const val SHOT_MISSING = "Пост ещё не отрисован. Выберите его снова."
internal const val SHOT_WIDTH_CHANGED = "Ширина экрана изменилась. Выберите посты заново."
