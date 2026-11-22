package com.solthariel.pawchan.library

/** Russian plurals for the number of copies in the destructive confirmation. */
internal fun copyDeletionLead(count: Int): String {
    val lastTwo = count % HUNDRED
    val last = count % TEN
    val noun =
        when {
            lastTwo in ELEVEN..FOURTEEN -> "копий"
            last == 1 -> "копия"
            last in 2..FOUR -> "копии"
            else -> "копий"
        }
    val verb = if (noun == "копия") "Будет удалена" else "Будут удалены"
    return "$verb $count $noun."
}

private const val TEN = 10
private const val HUNDRED = 100
private const val ELEVEN = 11
private const val FOURTEEN = 14
private const val FOUR = 4
