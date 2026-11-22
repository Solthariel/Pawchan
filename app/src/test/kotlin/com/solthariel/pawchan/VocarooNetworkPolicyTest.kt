package com.solthariel.pawchan

import com.solthariel.pawchan.infrastructure.vocaroo.vocarooUsesCronet
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocarooNetworkPolicyTest {
    @Test fun cronetAlternatesWithOkHttpOnlyWithoutProxy() {
        assertTrue(vocarooUsesCronet(1))
        assertFalse(vocarooUsesCronet(2))
        assertTrue(vocarooUsesCronet(3))
        assertFalse(vocarooUsesCronet(1, proxyEnabled = true))
        assertFalse(vocarooUsesCronet(2, proxyEnabled = true))
    }
}
