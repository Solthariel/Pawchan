package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class MirrorReadRetryTest {
    @Test fun failedReadRetriesExactlyOnceAndNeverCopiesCredentials() {
        var probes = 0
        val bare = OkHttpClient()
        val recovery =
            MirrorRecovery("2ch.su", { host, report ->
                probes++
                report(MirrorObservation(host, MirrorAvailability.AVAILABLE, 1))
                bare.newCall(Request.Builder().url("https://$host/index.json").build())
            }, {})
        val seen = mutableListOf<Request>()
        val client =
            bare
                .newBuilder()
                .addInterceptor(MirrorRoutingInterceptor("2ch.su", recovery))
                .addInterceptor { chain ->
                    seen.add(chain.request())
                    if (seen.size == 1) throw IOException("network unavailable")
                    response(chain.request(), 200)
                }.build()
        val request =
            Request
                .Builder()
                .url("https://2ch.su/b/index.json")
                .header("Cookie", "secret")
                .header("Authorization", "secret")
                .build()
        client.newCall(request).execute().close()
        assertEquals(2, seen.size)
        assertEquals("2ch.org", seen.last().url.host)
        assertNull(seen.last().header("Cookie"))
        assertNull(seen.last().header("Authorization"))
        assertEquals(2, probes)
        recovery.close()
    }

    @Test fun postsAndProtectedErrorsNeverTriggerAProbeOrReplay() {
        val bare = OkHttpClient()
        var probes = 0
        val recovery =
            MirrorRecovery("2ch.su", { host, _ ->
                probes++
                bare.newCall(Request.Builder().url("https://$host/index.json").build())
            }, {})
        for (status in listOf(404, 429, 403, 401, 200)) {
            var calls = 0
            val client =
                bare
                    .newBuilder()
                    .addInterceptor(MirrorRoutingInterceptor("2ch.su", recovery))
                    .addInterceptor { chain ->
                        calls++
                        response(chain.request(), status)
                    }.build()
            client.newCall(Request.Builder().url("https://2ch.su/b/index.json").build()).execute().close()
            assertEquals(1, calls)
        }
        var posts = 0
        val client =
            bare
                .newBuilder()
                .addInterceptor(MirrorRoutingInterceptor("2ch.su", recovery))
                .addInterceptor { chain ->
                    posts++
                    response(chain.request(), 503)
                }.build()
        client
            .newCall(
                Request
                    .Builder()
                    .url("https://2ch.su/user/posting")
                    .post("post".toRequestBody())
                    .build(),
            ).execute()
            .close()
        assertEquals(1, posts)
        assertEquals(0, probes)
        recovery.close()
    }

    @Test fun mediaAndExternalRequestsAreNotEligible() {
        for (url in listOf("https://2ch.su/b/src/video.mp4", "https://external.example/test.json")) {
            assertFalse(
                Request
                    .Builder()
                    .url(url)
                    .build()
                    .isMirrorRead(),
            )
        }
        assertFalse(
            Request
                .Builder()
                .url("https://2ch.su/b/index.json")
                .header("Range", "bytes=0-10")
                .build()
                .isMirrorRead(),
        )
    }

    @Test fun secondFailureIsReturnedWithoutAnotherRetry() {
        val bare = OkHttpClient()
        val recovery =
            MirrorRecovery("2ch.su", { host, report ->
                report(MirrorObservation(host, MirrorAvailability.AVAILABLE, 1))
                bare.newCall(Request.Builder().url("https://$host/index.json").build())
            }, {})
        var calls = 0
        val client =
            bare
                .newBuilder()
                .addInterceptor(MirrorRoutingInterceptor("2ch.su", recovery))
                .addInterceptor { chain ->
                    calls++
                    response(chain.request(), 503)
                }.build()
        client.newCall(Request.Builder().url("https://2ch.su/b/index.json").build()).execute().use {
            assertEquals(503, it.code)
        }
        assertEquals(2, calls)
        recovery.close()
    }

    private fun response(
        request: Request,
        code: Int,
    ): Response =
        Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body("{}".toResponseBody())
            .build()
}
