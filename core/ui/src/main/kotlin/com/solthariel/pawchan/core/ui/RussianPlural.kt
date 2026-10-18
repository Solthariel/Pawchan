package com.solthariel.pawchan.core.ui

/** Русское склонение после числа: 1 ответ, 2 ответа, 5 ответов, 21 ответ, 11 ответов. */
fun russianPlural(
    count: Int,
    one: String,
    few: String,
    many: String,
): String {
    val tens = count % HUNDRED
    val units = count % TEN
    val word =
        when {
            tens in TEENS -> many
            units == 1 -> one
            units in FEW -> few
            else -> many
        }
    return "$count $word"
}

private const val TEN = 10
private const val HUNDRED = 100
private const val TEEN_FIRST = 11
private const val TEEN_LAST = 14
private const val FEW_LAST = 4
private val TEENS = TEEN_FIRST..TEEN_LAST
private val FEW = 2..FEW_LAST
