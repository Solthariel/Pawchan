package com.solthariel.pawchan.data.network

import org.jsoup.Jsoup

internal fun String.decodeHtmlText(): String = Jsoup.parseBodyFragment(this).text()

internal fun String.withoutWhitespace(): String = replace(WHITESPACE, "")

private val WHITESPACE = Regex("\\s")
