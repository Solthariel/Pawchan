package com.solthariel.pawchan.media.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.solthariel.pawchan.media.MediaViewerNavigation

/** All page controls, swipes and interrupted committed swipes snapshot before removing the old view. */
@Composable
internal fun rememberPosterNavigation(
    player: VideoPlaybackSession,
    navigation: MediaViewerNavigation,
): MediaViewerNavigation {
    val store = LocalVideoPosterStore.current
    return remember(player, navigation, store) {
        navigation.copy(
            previous =
                navigation.previous?.let { action ->
                    {
                        store?.capture(player)
                        action()
                    }
                },
            next =
                navigation.next?.let { action ->
                    {
                        store?.capture(player)
                        action()
                    }
                },
        )
    }
}
