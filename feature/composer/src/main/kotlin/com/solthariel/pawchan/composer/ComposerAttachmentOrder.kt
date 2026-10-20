package com.solthariel.pawchan.composer

/** Move, do not swap: all intervening attachments retain their relative order and identity. */
internal fun <T> moveComposerItem(
    items: List<T>,
    from: Int,
    to: Int,
): List<T> =
    if (from !in items.indices || to !in items.indices || from == to) {
        items
    } else {
        items.toMutableList().apply { add(to, removeAt(from)) }
    }

internal data class ComposerDropGrid(
    val width: Float,
    val cellHeight: Float,
    val gap: Float,
    val rtl: Boolean = false,
) {
    val cellWidth: Float get() = ((width - gap * (COLUMNS - 1)) / COLUMNS).coerceAtLeast(0f)

    fun left(index: Int): Float {
        val column = index % COLUMNS
        return (if (rtl) COLUMNS - 1 - column else column) * (cellWidth + gap)
    }

    fun top(index: Int): Float = index / COLUMNS * (cellHeight + gap)

    fun indexAt(
        x: Float,
        y: Float,
        count: Int,
    ): Int? {
        if (count <= 0 || !validGeometry()) return null
        val rows = (count + COLUMNS - 1) / COLUMNS
        val height = rows * (cellHeight + gap) - gap
        return if (x in 0f..width && y in 0f..height) {
            val visualColumn = ((x + gap / 2) / (cellWidth + gap)).toInt().coerceIn(0, COLUMNS - 1)
            val column = if (rtl) COLUMNS - 1 - visualColumn else visualColumn
            val row = ((y + gap / 2) / (cellHeight + gap)).toInt().coerceIn(0, rows - 1)
            (row * COLUMNS + column).coerceAtMost(count - 1)
        } else {
            null
        }
    }

    private fun validGeometry(): Boolean = cellWidth > 0 && cellHeight > 0
}

private const val COLUMNS = 4
