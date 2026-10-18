package com.solthariel.pawchan.domain.filter

import java.util.Locale

internal class FilterText(
    val source: String,
) {
    private val starts = mutableListOf<Int>()
    private val ends = mutableListOf<Int>()
    val normalized: String =
        buildString {
            source.forEachIndexed { index, character ->
                if (character.isWhitespace() || Character.isSpaceChar(character)) {
                    if (isNotEmpty() && last() != ' ') {
                        append(' ')
                        starts.add(index)
                        ends.add(index)
                    } else if (isNotEmpty()) {
                        ends[ends.lastIndex] = index
                    }
                } else {
                    val lowered = character.toString().lowercase(Locale.ROOT).replace('ё', 'е')
                    lowered.forEach {
                        append(it)
                        starts.add(index)
                        ends.add(index)
                    }
                }
            }
            if (isNotEmpty() && last() == ' ') {
                deleteCharAt(lastIndex)
                starts.removeAt(starts.lastIndex)
                ends.removeAt(ends.lastIndex)
            }
        }

    fun original(range: IntRange): IntRange = starts[range.first]..ends[range.last]
}

internal fun normalizedFilterText(text: String): String =
    text
        .lowercase(Locale.ROOT)
        .replace('ё', 'е')
        .replace(FILTER_WHITESPACE, " ")
        .trim()

private val FILTER_WHITESPACE = Regex("[\\s\\p{Z}]+")

internal fun wordCharacter(character: Char): Boolean = character.isLetterOrDigit() || character == '_'
