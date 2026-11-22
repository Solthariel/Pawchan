package com.solthariel.pawchan.composer

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier

/** Only explicit paste/IME/drop events are handled; never polls the user's clipboard. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.composerMediaReceiver(onReceive: (List<Uri>, Any?) -> Unit): Modifier {
    val current = rememberUpdatedState(onReceive)
    val listener =
        remember {
            ReceiveContentListener { content ->
                receiveComposerImages(content) { uris -> current.value(uris, content) }
            }
        }
    return this.contentReceiver(listener)
}

@OptIn(ExperimentalFoundationApi::class)
internal fun receiveComposerImages(
    content: TransferableContent,
    onReceive: (List<Uri>) -> Unit,
): TransferableContent? {
    if (!content.hasMediaType(MediaType.Image)) return content
    val uris = mutableListOf<Uri>()
    val remaining =
        content.consume { item ->
            val uri = item.uri
            if (uri?.scheme == "content") {
                uris.add(uri)
                true
            } else {
                false
            }
        }
    if (uris.isNotEmpty()) onReceive(uris)
    return remaining
}
