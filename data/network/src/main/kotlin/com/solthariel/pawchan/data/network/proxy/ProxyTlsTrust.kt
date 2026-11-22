package com.solthariel.pawchan.data.network.proxy

import okhttp3.OkHttpClient
import java.io.IOException
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
        } catch (failure: SSLException) {
            throw ProxyTunnelException(failure.tlsFailure(), "TLS handshake with the proxy failed", failure)
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

private val MODERN_PROTOCOLS = listOf("TLSv1.3", "TLSv1.2")
private const val TLS_CAUSE_DEPTH = 8
