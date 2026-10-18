package com.solthariel.pawchan.infrastructure.vocaroo

import android.util.Log
import com.solthariel.pawchan.thread.vocaroo.VocarooRequest
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Protocol
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Журнал одной попытки Vocaroo (тег `Vocaroo`, пишется и в release): запись, номер попытки,
 * время подключения/TLS/загрузки, размер, адрес сервера и стадия, на которой случилась ошибка.
 */
internal class VocarooEventLog(
    call: Call,
) : EventListener() {
    private val request = call.request().tag(VocarooRequest::class.java)
    private val label = "record=${request?.id ?: "?"} attempt=${request?.attempt ?: 0} okhttp"
    private val started = System.nanoTime()
    private var stageStarted = started
    private var server: InetSocketAddress? = null
    private var stage = "start"
        set(value) {
            field = value
            stageStarted = System.nanoTime()
        }

    override fun callStart(call: Call) = info("$label start ${call.request().url}")

    override fun dnsStart(
        call: Call,
        domainName: String,
    ) {
        stage = "dns"
    }

    override fun connectStart(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
    ) {
        server = inetSocketAddress
        stage = "connect"
    }

    override fun secureConnectStart(call: Call) {
        info("$label tcp ${server?.address?.hostAddress} ${elapsedMillis(stageStarted)}ms")
        stage = "tls"
    }

    override fun secureConnectEnd(
        call: Call,
        handshake: Handshake?,
    ) = info("$label tls ${handshake?.tlsVersion} ${elapsedMillis(stageStarted)}ms")

    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException,
    ) = warn("$label $stage failed ${inetSocketAddress.address?.hostAddress}: ${ioe.javaClass.simpleName}")

    override fun connectionAcquired(
        call: Call,
        connection: Connection,
    ) {
        val address = server?.address?.hostAddress
        info("$label connection $address ${connection.protocol()}")
        stage = "request"
    }

    override fun responseHeadersEnd(
        call: Call,
        response: Response,
    ) {
        info("$label http ${response.code} after ${elapsedMillis(started)}ms")
        stage = "download"
    }

    override fun responseBodyEnd(
        call: Call,
        byteCount: Long,
    ) = info("$label body ${byteCount}B in ${elapsedMillis(stageStarted)}ms, total ${elapsedMillis(started)}ms")

    override fun callFailed(
        call: Call,
        ioe: IOException,
    ) {
        val address = server?.address?.hostAddress
        val reason = "${ioe.javaClass.simpleName}: ${ioe.message}"
        warn("$label failed at $stage after ${elapsedMillis(started)}ms server=$address: $reason")
    }

    override fun canceled(call: Call) = info("$label canceled at $stage after ${elapsedMillis(started)}ms")
}

private fun info(message: String) {
    Log.i(TAG, message)
}

private fun warn(message: String) {
    Log.w(TAG, message)
}
