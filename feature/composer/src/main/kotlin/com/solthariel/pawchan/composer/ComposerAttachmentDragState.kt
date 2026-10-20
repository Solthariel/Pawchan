package com.solthariel.pawchan.composer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/** Reordering is local while dragging. Commit once on release; cancellation leaves the draft untouched. */
internal class ComposerAttachmentDragState {
    var id by mutableStateOf<String?>(null)
        private set
    private var order by mutableStateOf(emptyList<String>())
    private var position by mutableStateOf(Offset.Zero)
    private var anchor = Offset.Zero
    val topLeft: Offset get() = position - anchor

    fun begin(
        files: List<ComposerAttachment>,
        grid: ComposerDropGrid,
        point: Offset,
    ): Boolean {
        val index = grid.indexAt(point.x, point.y, files.size) ?: return false
        id = files[index].id
        order = files.map { it.id }
        position = point
        anchor = point - Offset(grid.left(index), grid.top(index))
        return true
    }

    fun move(
        grid: ComposerDropGrid,
        point: Offset,
    ) {
        position = point
        val center = topLeft + Offset(grid.cellWidth / 2, grid.cellHeight / 2)
        val target = grid.indexAt(center.x, center.y, order.size) ?: return
        order = moveComposerItem(order, order.indexOf(id), target)
    }

    fun arrange(files: List<ComposerAttachment>): List<ComposerAttachment> =
        if (matches(files)) files.sortedBy { order.indexOf(it.id) } else files

    fun matches(files: List<ComposerAttachment>): Boolean =
        order.size == files.size &&
            order.toSet() == files.map { it.id }.toSet()

    fun finish() {
        id = null
        order = emptyList()
        position = Offset.Zero
        anchor = Offset.Zero
    }
}
