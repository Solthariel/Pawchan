package com.solthariel.pawchan.thread

internal enum class ThreadBackTarget { MEDIA, DIALOG, SELECTION, SEARCH, ROUTE }

internal fun threadBackTarget(
    mediaOpened: Boolean,
    dialogOpen: Boolean,
    selecting: Boolean,
    searchOpen: Boolean,
): ThreadBackTarget =
    when {
        mediaOpened -> ThreadBackTarget.MEDIA
        dialogOpen -> ThreadBackTarget.DIALOG
        selecting -> ThreadBackTarget.SELECTION
        searchOpen -> ThreadBackTarget.SEARCH
        else -> ThreadBackTarget.ROUTE
    }
