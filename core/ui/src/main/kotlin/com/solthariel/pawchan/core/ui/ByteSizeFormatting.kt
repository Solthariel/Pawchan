package com.solthariel.pawchan.core.ui

fun formatByteSizeForUi(bytes: Long): String {
    val units = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= BYTES_PER_DISPLAY_UNIT && unit < units.lastIndex) {
        value /= BYTES_PER_DISPLAY_UNIT
        unit++
    }
    return if (unit == 0) {
        "$bytes ${units[unit]}"
    } else {
        val formatted =
            if (value >= INTEGER_FILE_SIZE_THRESHOLD) {
                value.toInt().toString()
            } else {
                "%.1f".format(java.util.Locale.ROOT, value)
            }
        "$formatted ${units[unit]}"
    }
}

private const val BYTES_PER_DISPLAY_UNIT = 1000.0
private const val INTEGER_FILE_SIZE_THRESHOLD = 10.0
