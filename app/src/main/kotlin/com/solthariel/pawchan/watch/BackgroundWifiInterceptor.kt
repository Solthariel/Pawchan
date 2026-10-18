package com.solthariel.pawchan.watch

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

internal class BackgroundWifiInterceptor(
    private val context: Context,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!backgroundNetworkAllowed(context, true)) throw IOException("Background request requires Wi-Fi")
        return chain.proceed(chain.request())
    }
}
