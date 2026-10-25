package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RateLimitIdentityRegressionTest {
    @Test
    fun differentPostingCooldownsDoNotCollapseToTheSameFailure() =
        runTest {
            val short = executePostingRequest(blockedClient("30"), request()) { PostingOutcome.Success("ignored") }
            val long = executePostingRequest(blockedClient("120"), request()) { PostingOutcome.Success("ignored") }
            assertNotEquals(short, long)
        }

    @Test
    fun differentApiCooldownsDoNotCollapseToTheSameFailure() =
        runTest {
            val short = executeDomainRequest(blockedClient("30"), request()) { "ignored" }
            val long = executeDomainRequest(blockedClient("120"), request()) { "ignored" }
            assertNotEquals(short, long)
        }

    private fun blockedClient(seconds: String): OkHttpClient {
        val policy = ServerRequestBackoff({ 0L }, { 0L })
        policy.rateLimited("2ch.su", seconds, null)
        return OkHttpClient.Builder().addInterceptor(ServerBackoffInterceptor(policy)).build()
    }

    private fun request(): Request = Request.Builder().url("https://2ch.su/user/posting").build()
}
