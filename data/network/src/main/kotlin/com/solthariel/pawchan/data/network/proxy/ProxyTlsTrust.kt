package com.solthariel.pawchan.data.network.proxy

import okhttp3.OkHttpClient
import java.io.IOException
import java.io.InterruptedIOException
import java.net.Socket
import java.security.cert.CertificateException
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * How the certificate of an HTTPS proxy is judged. The default is the system trust store plus
 * strict host name matching; tests inject their own trust anchor, nothing else may weaken it.
 */
class ProxyTlsTrust(
    private val socketFactory: SSLSocketFactory,
    private val hostnameVerifier: HostnameVerifier,
) {
    /** Starts TLS over [plain], verifying the chain during the handshake and the name right after it. */
    internal fun connect(
        plain: Socket,
        endpoint: ProxyEndpoint,
    ): SSLSocket {
        val tls = socketFactory.createSocket(plain, endpoint.host, endpoint.port, true) as SSLSocket
        try {
            tls.enabledProtocols = MODERN_PROTOCOLS.filter { it in tls.supportedProtocols }.toTypedArray()
            handshake(tls)
            verifyHostname(tls, endpoint.host)
        } catch (failure: IOException) {
            tls.close()
            throw failure
        }
        return tls
    }

    private fun handshake(tls: SSLSocket) {
        try {
            tls.startHandshake()
        } catch (failure: IOException) {
            throw handshakeFailure(failure)
        }
    }

    private fun verifyHostname(
        tls: SSLSocket,
        host: String,
    ) {
        if (!hostnameVerifier.verify(host, tls.session)) {
            throw ProxyTunnelException(ProxyFailure.TLS_HOSTNAME, "The proxy certificate is for another host name")
        }
    }

    companion object {
        /** System trust store, as for every other HTTPS connection of the app. */
        fun system(): ProxyTlsTrust = SYSTEM

        private val SYSTEM by lazy {
            ProxyTlsTrust(SSLSocketFactory.getDefault() as SSLSocketFactory, OkHttpClient().hostnameVerifier)
        }
    }
}

private fun SSLException.tlsFailure(): ProxyFailure =
    if (generateSequence<Throwable>(this) { it.cause }.take(TLS_CAUSE_DEPTH).any { it is CertificateException }) {
        ProxyFailure.TLS_UNTRUSTED
    } else {
        ProxyFailure.TLS_FAILED
    }

/**
 * A rejected certificate is untrusted, any other TLS error a failed handshake. A connection the server
 * resets or drops mid-handshake is a failed handshake too: some TLS providers report it as an
 * [SSLException], others (the JDK one among them) pass the bare socket error on, and on Windows a reset
 * also discards whatever the server had sent before it. A timeout stays a timeout for the caller.
 */
private fun handshakeFailure(failure: IOException): IOException =
    when (failure) {
        is SSLException -> {
            ProxyTunnelException(failure.tlsFailure(), "TLS handshake with the proxy failed", failure)
        }

        is InterruptedIOException -> {
            failure
        }

        else -> {
            ProxyTunnelException(ProxyFailure.TLS_FAILED, "The proxy dropped the TLS handshake", failure)
        }
    }

private val MODERN_PROTOCOLS = listOf("TLSv1.3", "TLSv1.2")
private const val TLS_CAUSE_DEPTH = 8
