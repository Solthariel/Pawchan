package com.solthariel.pawchan.navigation

import androidx.navigation.NavController
import com.solthariel.pawchan.domain.model.ThreadAddress

internal fun NavController.navigateThread(address: ThreadAddress) {
    val current = currentBackStackEntry
    val sameThread =
        current?.destination?.route == AppRoute.THREAD &&
            current.arguments?.toThreadAddress() == address
    if (sameThread) return
    // Все треды используют один destination. SingleTop заменил бы верхнюю запись
    // даже при другом адресе: A -> B -> C должно возвращаться как C -> B -> A.
    navigate(AppRoute.thread(address))
}

internal fun NavController.completeChallenge() {
    val handle = previousBackStackEntry?.savedStateHandle
    val nextToken = (handle?.get<Long>(AppRoute.CHALLENGE_RETRY_TOKEN) ?: 0L) + 1L
    handle?.set(AppRoute.CHALLENGE_RETRY_TOKEN, nextToken)
    popBackStack()
}
