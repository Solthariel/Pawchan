package com.solthariel.pawchan.domain.drafts

data class MediaDraftOptions(
    val uniqueHash: Boolean = false,
    val removeMetadata: Boolean = false,
    val removeFileName: Boolean = false,
    val customName: String? = null,
    val reencoding: DraftImageReencoding? = null,
    val spoiler: Boolean = false,
    val previousReencoding: DraftImageReencoding? = null,
)

data class DraftImageReencoding(
    val jpeg: Boolean = true,
    val quality: Int = 100,
    val scalePercent: Int = 100,
    val scaleDivisor: Int = 1,
) {
    init {
        require(quality in 1..FULL_PERCENT)
        require(scalePercent in 1..FULL_PERCENT)
        require(scaleDivisor in listOf(1, HALF_SIZE_DIVISOR, QUARTER_SIZE_DIVISOR, EIGHTH_SIZE_DIVISOR))
    }

    // Keep legacy percentage drafts; new controls use an exact integer divisor.
    val scaleFactor: Float get() = scalePercent.toFloat() / FULL_PERCENT / scaleDivisor
}

private const val FULL_PERCENT = 100
private const val HALF_SIZE_DIVISOR = 2
private const val QUARTER_SIZE_DIVISOR = 4
private const val EIGHTH_SIZE_DIVISOR = 8
