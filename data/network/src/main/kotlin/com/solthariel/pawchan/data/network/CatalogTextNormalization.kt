package com.solthariel.pawchan.data.network

import org.jsoup.Jsoup

internal fun String.decodeHtmlText(): String = Jsoup.parseBodyFragment(this).text()
