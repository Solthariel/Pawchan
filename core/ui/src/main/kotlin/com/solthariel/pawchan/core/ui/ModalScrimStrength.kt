package com.solthariel.pawchan.core.ui

/** Historical normal dim and the user-selected stronger media dim, each drawn once. */
enum class ModalScrimStrength {
    STANDARD,
    MEDIA,
    NONE,
    ;

    fun alpha(): Float =
        when (this) {
            STANDARD -> MODAL_SCRIM_ALPHA
            MEDIA -> MEDIA_SCRIM_ALPHA
            NONE -> 0f
        }
}

/** Theme.Material backgroundDimAmount inherited by the original native dialogs. */
internal const val MODAL_SCRIM_ALPHA = 0.6f

/** User-selected rounding of the old combined media backdrop (approximately 88.8%). */
private const val MEDIA_SCRIM_ALPHA = 0.9f
