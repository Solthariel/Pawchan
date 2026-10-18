package com.solthariel.pawchan.navigation

import android.os.Bundle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress

internal fun boardArguments(): List<NamedNavArgument> =
    listOf(
        navArgument(AppRoute.SITE_ARGUMENT) { type = NavType.StringType },
        navArgument(AppRoute.BOARD_ARGUMENT) { type = NavType.StringType },
    )

internal fun threadArguments(): List<NamedNavArgument> =
    boardArguments() + navArgument(AppRoute.THREAD_ARGUMENT) { type = NavType.LongType }

internal fun Bundle.toBoardAddress(): BoardAddress? {
    val site = getString(AppRoute.SITE_ARGUMENT)
    val board = getString(AppRoute.BOARD_ARGUMENT)
    return if (site != null && board != null) boardAddress(site, board) else null
}

internal fun Bundle.toThreadAddress(): ThreadAddress? {
    val site = getString(AppRoute.SITE_ARGUMENT)
    val board = getString(AppRoute.BOARD_ARGUMENT)
    return if (site != null && board != null) {
        threadAddress(site, board, getLong(AppRoute.THREAD_ARGUMENT))
    } else {
        null
    }
}
