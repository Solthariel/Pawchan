package com.solthariel.pawchan.infrastructure.vocaroo

import android.content.Context
import com.solthariel.pawchan.data.network.proxy.proxyRouter
import com.solthariel.pawchan.thread.vocaroo.OkHttpVocarooTransport
import com.solthariel.pawchan.thread.vocaroo.VocarooRequest
import com.solthariel.pawchan.thread.vocaroo.VocarooTransport
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File
import java.time.Duration

/**
 * Сеть Vocaroo принадлежит [com.solthariel.pawchan.infrastructure.AppNetworkStack]: свой диспетчер
 * (запись не ждёт в очереди за миниатюрами), журнал попыток и Cronet. Соединение
 * переиспользуется между попытками и записями, пока живёт стек.
 */
internal class VocarooNetwork(
    context: Context,
    contentClient: OkHttpClient,
) : VocarooTransport,
    AutoCloseable {
    private val client =
        contentClient
            .newBuilder()
            .dispatcher(Dispatcher())
            .eventListenerFactory(::VocarooEventLog)
            .connectTimeout(CONNECT_TIMEOUT)
            .readTimeout(READ_TIMEOUT)
            .callTimeout(CALL_TIMEOUT)
            .build()
    private val okHttp = OkHttpVocarooTransport(client)
    private val cronet = VocarooCronet(context)

    override suspend fun download(
        request: VocarooRequest,
        target: File,
        onProgress: (Float?) -> Unit,
    ) {
        val viaCronet =
            vocarooUsesCronet(request.attempt, proxyEnabled()) &&
                cronet.ready(CRONET_WAIT_MILLIS) && !proxyEnabled()
        (if (viaCronet) cronet else okHttp).download(request, target, onProgress)
    }

    private fun proxyEnabled(): Boolean {
        val selector = client.proxyRouter()
        return selector != null &&
            selector.route(com.solthariel.pawchan.domain.proxy.ProxyService.VOCAROO) !=
            com.solthariel.pawchan.data.network.proxy.ProxyRoute.Direct
    }

    override fun close() {
        client.dispatcher.cancelAll()
        client.dispatcher.executorService.shutdown()
        cronet.close()
    }
}

/**
 * Без прокси нечётные попытки идут через Cronet, чётные — через OkHttp: если один стек
 * упирается в TLS или маршрут, следующая попытка пробует другой.
 */
internal fun vocarooUsesCronet(
    attempt: Int,
    proxyEnabled: Boolean = false,
): Boolean = !proxyEnabled && attempt % 2 == 1

private const val CRONET_WAIT_MILLIS = 1_500L
private const val CONNECT_TIMEOUT_SECONDS = 5L
private const val READ_TIMEOUT_SECONDS = 10L
private const val CALL_TIMEOUT_SECONDS = 20L
private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS)
private val READ_TIMEOUT: Duration = Duration.ofSeconds(READ_TIMEOUT_SECONDS)
private val CALL_TIMEOUT: Duration = Duration.ofSeconds(CALL_TIMEOUT_SECONDS)
