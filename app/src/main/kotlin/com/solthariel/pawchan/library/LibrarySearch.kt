package com.solthariel.pawchan.library

import com.solthariel.pawchan.catalog.CatalogThreadUi

/** Поиск по копиям: без учёта регистра, по заголовку, тексту ОП-поста, доске и номеру. */
internal fun CatalogThreadUi.matches(query: String): Boolean =
    query.isEmpty() ||
        listOfNotNull(subject, comment, "/${address.board.board.value}/", id.toString())
            .any { it.contains(query, ignoreCase = true) }

/** Поиск по истории: заголовок, доска и номер треда. */
internal fun LibraryEntryUi.matches(query: String): Boolean =
    query.isEmpty() ||
        listOfNotNull(subject, "/$board/", threadId.toString()).any { it.contains(query, ignoreCase = true) }
