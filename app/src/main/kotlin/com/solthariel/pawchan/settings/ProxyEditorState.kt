package com.solthariel.pawchan.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.data.network.proxy.proxyEndpointOf
import com.solthariel.pawchan.data.network.proxy.proxyRouteFor
import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.solthariel.pawchan.domain.proxy.SavedProxy

internal class ProxyEditorState(
    private val initial: SavedProxy,
) {
    private val endpoint = proxyRouteFor(initial.mode, initial.address, initial.credentials)?.endpoint
    var draft by mutableStateOf(initial)
    var host by mutableStateOf(endpoint?.host.orEmpty())
    var port by mutableStateOf(endpoint?.port?.toString().orEmpty())
    var login by mutableStateOf(initial.credentials?.username.orEmpty())
    var password by mutableStateOf(initial.credentials?.password.orEmpty())
    var passwordVisible by mutableStateOf(false)
    var deleting by mutableStateOf(false)
    var discarding by mutableStateOf(false)

    fun changeHost(value: String) {
        val parts = pastedProxyAuthority(host, value)
        if (parts == null) {
            host = value
        } else {
            host = parts.first
            port = parts.second
        }
    }

    val current: SavedProxy get() {
        val address = proxyEndpointOf(host, port)?.authority.orEmpty()
        val credentials = if (login.isEmpty() && password.isEmpty()) null else ProxyCredentials(login, password)
        return draft.copy(address = address, credentials = credentials)
    }
    val dirty: Boolean
        get() {
            val incomplete = current.address.isEmpty() && (host.isNotEmpty() || port.isNotEmpty())
            return current != initial || incomplete
        }
}
