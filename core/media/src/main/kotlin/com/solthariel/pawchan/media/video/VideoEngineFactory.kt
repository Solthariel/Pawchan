package com.solthariel.pawchan.media.video

import android.content.Context
import okhttp3.OkHttpClient
import java.io.File

internal fun videoEngine(
    context: Context,
    client: OkHttpClient,
    directory: File,
): VideoEngineBackend = MpvVideoEngine(context, client, directory)
