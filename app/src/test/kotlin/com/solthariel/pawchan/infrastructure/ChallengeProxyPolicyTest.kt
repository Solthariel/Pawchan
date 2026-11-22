package com.solthariel.pawchan.infrastructure

import com.solthariel.pawchan.data.network.proxy.ProxyRuntimeStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeProxyPolicyTest {
    @Test fun webViewOnlyOpensOnDirectPolicy() {
        assertTrue(challengeAllowedForProxy(ProxyRuntimeStatus.DIRECT))
        ProxyRuntimeStatus.entries.filter { it != ProxyRuntimeStatus.DIRECT }.forEach {
            assertFalse(it.name, challengeAllowedForProxy(it))
        }
    }
}
