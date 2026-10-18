package com.solthariel.pawchan.watch

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService

internal fun backgroundNetworkAllowed(
    context: Context,
    wifiOnly: Boolean,
): Boolean {
    val manager = context.getSystemService<ConnectivityManager>()
    val network = manager?.activeNetwork
    val wifi =
        network
            ?.let { manager.getNetworkCapabilities(it) }
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    return !wifiOnly || wifi
}
