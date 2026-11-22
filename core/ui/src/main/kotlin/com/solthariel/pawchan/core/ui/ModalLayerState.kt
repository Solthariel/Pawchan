package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

internal enum class ModalLayerKind { WINDOW, POPUP, INLINE, FULL_SCREEN, INLINE_PANEL }

internal data class ModalLayerEntry(
    val token: Any,
    val kind: ModalLayerKind,
    val strength: ModalScrimStrength = ModalScrimStrength.STANDARD,
    val panel: ModalPanelGeometry? = null,
    val replacesPageScrim: Boolean = false,
)

internal data class ModalPanelGeometry(
    val bounds: Rect,
    val radius: Float,
    val additional: Rect? = null,
)

/** Owned by the UI host, never shared across activities or saved as persistent application data. */
internal class ModalLayerState(
    val hosted: Boolean,
) {
    private var entries by mutableStateOf(emptyList<ModalLayerEntry>())

    fun enter(entry: ModalLayerEntry) {
        check(entries.none { it.token === entry.token })
        entries = entries + entry
    }

    fun leave(token: Any) {
        entries = entries.filterNot { it.token === token }
    }

    fun covered(token: Any): Boolean {
        val index = entries.indexOfFirst { it.token === token }
        return index >= 0 && index < entries.lastIndex
    }

    fun pageAlpha(): Float = if (dimPage() && !pageScrimReplaced) entries.first().strength.alpha() else 0f

    val pageScrimReplaced: Boolean get() = entries.any { it.replacesPageScrim }

    fun coveredAlpha(token: Any): Float {
        val index = entries.indexOfFirst { it.token === token }
        return if (index >= 0 && index < entries.lastIndex) entries[index + 1].strength.alpha() else 0f
    }

    fun systemBarAlpha(): Float =
        when (entries.firstOrNull()?.kind) {
            ModalLayerKind.FULL_SCREEN -> entries.getOrNull(1)?.strength?.alpha() ?: 0f
            ModalLayerKind.INLINE -> MODAL_SCRIM_ALPHA
            else -> pageAlpha()
        }

    fun panel(): ModalPanelGeometry? {
        val first = entries.firstOrNull()
        return if (first?.kind == ModalLayerKind.INLINE_PANEL) first.panel else null
    }

    fun updatePanel(
        token: Any,
        geometry: ModalPanelGeometry,
    ) {
        entries =
            entries.map {
                if (it.token === token) {
                    val next = geometry.copy(additional = it.panel?.additional)
                    if (it.panel != next) it.copy(panel = next) else it
                } else {
                    it
                }
            }
    }

    fun updateAdditionalPanel(
        token: Any,
        bounds: Rect,
    ) {
        entries =
            entries.map {
                if (it.token === token) {
                    val next = (it.panel ?: ModalPanelGeometry(Rect.Zero, 0f)).copy(additional = bounds)
                    if (it.panel != next) it.copy(panel = next) else it
                } else {
                    it
                }
            }
    }

    fun first(token: Any): Boolean = entries.firstOrNull()?.token === token

    fun dimPage(): Boolean =
        when (entries.firstOrNull()?.kind) {
            ModalLayerKind.WINDOW, ModalLayerKind.POPUP, ModalLayerKind.INLINE_PANEL -> true
            else -> false
        }
}
