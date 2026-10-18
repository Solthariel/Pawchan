package com.solthariel.pawchan.media

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import com.solthariel.pawchan.domain.settings.ImagePreload

@Composable
internal fun neighborOriginalsAllowed(): Boolean {
    val policy = LocalMediaPreferences.current.preloadImages
    val manager = LocalContext.current.getSystemService<ConnectivityManager>()

    fun wifi(): Boolean =
        manager
            ?.activeNetwork
            ?.let { manager.getNetworkCapabilities(it) }
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    var onWifi by remember(manager) { mutableStateOf(wifi()) }
    DisposableEffect(manager) {
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onWifi = wifi()
                }

                override fun onLost(network: Network) {
                    onWifi = wifi()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    caps: NetworkCapabilities,
                ) {
                    onWifi = wifi()
                }
            }
        manager?.registerDefaultNetworkCallback(callback)
        onDispose { manager?.unregisterNetworkCallback(callback) }
    }
    return when (policy) {
        ImagePreload.NEVER -> false
        ImagePreload.WIFI -> onWifi
        ImagePreload.ALWAYS -> true
    }
}
