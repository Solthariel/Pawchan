package com.solthariel.pawchan.data.network.proxy

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** What went wrong with a proxied request, in terms a person can act on. */
enum class ProxyFailure {
    /** Nothing answered in time. */
    TIMEOUT,

    /** The proxy server could not be reached (wrong address or port, refused, no network). */
    PROXY_UNREACHABLE,

    /** The proxy wants a login and none is set. */
    AUTH_REQUIRED,

    /** The proxy refused the login or password. */
    AUTH_REJECTED,

    /** The HTTPS proxy certificate is not trusted, expired or invalid. */
    TLS_UNTRUSTED,

    /** The HTTPS proxy certificate belongs to another host name. */
    TLS_HOSTNAME,

    /** TLS could not be established with the proxy (it may not be an HTTPS proxy). */
    TLS_FAILED,

    /** The server on the proxy port does not speak the selected proxy protocol. */
    BAD_PROXY_RESPONSE,

    /** The proxy answered but refused or failed to reach the resource. */
    TARGET_UNREACHABLE,

    /** The tunnel is fine, but TLS with the resource itself failed. */
    RESOURCE_TLS,

    /** The proxy is still being applied or is not set up; nothing was sent. */
    NOT_READY,

    OTHER,
}

/** A proxy handshake failed. The message never contains a login, a password or a header with them. */
class ProxyTunnelException(
    val failure: ProxyFailure,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/** Resolving a resource name while its proxy is not ready. Nothing was sent anywhere. */
class ProxyBlockedException(
    message: String,
) : UnknownHostException(message)

/** Classifies by the first recognisable link of the cause chain. */
fun Throwable.proxyFailure(): ProxyFailure {
    val chain = generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).toList()
    chain.firstNotNullOfOrNull { (it as? ProxyTunnelException)?.failure }?.let { return it }
    return when {
        chain.any { it is ProxyBlockedException || it is StaleProxyConnectionException } -> {
            ProxyFailure.NOT_READY
        }

        chain.any { it is InterruptedIOException } -> {
            ProxyFailure.TIMEOUT
        }

        chain.any { it is SSLException } -> {
            ProxyFailure.RESOURCE_TLS
        }

        chain.any { it is ConnectException || it is NoRouteToHostException || it is UnknownHostException } -> {
            ProxyFailure.PROXY_UNREACHABLE
        }

        else -> {
            ProxyFailure.OTHER
        }
    }
}

private const val MAX_CAUSE_DEPTH = 8
