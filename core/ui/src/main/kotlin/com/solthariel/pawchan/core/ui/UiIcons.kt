package com.solthariel.pawchan.core.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

/** Bundled official Material Symbols Rounded, including post-state markers. */
@Composable
fun UiIcon(
    kind: UiIconKind,
    color: Color,
    modifier: Modifier,
) {
    Icon(uiIconPainter(kind), contentDescription = null, tint = color, modifier = modifier)
}

@Composable
fun uiIconPainter(kind: UiIconKind): Painter = painterResource(roundedIconResource(kind))

enum class UiIconKind {
    SEARCH,
    WRITE,
    BACK,
    UP,
    DOWN,
    REFRESH,
    MORE,
    MENU,
    GRID,
    DOCUMENT,
    BOOKMARK,
    CLOSE,
    SETTINGS,
    PIN,
    LOCK,
    MEDIA_MULTIPLE,
    INFINITY,
    SAVE,
    PLAY,
    VIDEO_PREVIEW_PLAY,
    PAUSE,
    PREVIOUS,
    NEXT,
    ROTATE,
    VOLUME_ON,
    VOLUME_OFF,
    PIP,
    MAIL,
    BANNED,
    WARNING,
    EXPAND,
    LINK,
    STAR,
    CHECK,
    TUNE,
    PALETTE,
    CHAT,
    IMAGE,
    FILTER,
    GLOBE,
    PUBLIC,
    CLEANING,
    INFO,
    ADD,
    VIDEO,
    CHECK_BOX,
    PERSON,
    SAGE,
    BOOKMARK_ADD,
    SEND,
    STORAGE,
    FORUM,
    REPLY,
    QUOTE,
    COPY,
    SHARE,
    DELETE,
    HIDE,
    SHOW,
    SELECT_ALL,
    ARCHIVE,
    HISTORY,
    HOME,
    TAG,
    CLEAR_FORMAT,
    UPLOAD,
    CODE,
    CHART,
    STAR_FILLED,
    BOOKMARK_FILLED,
}
