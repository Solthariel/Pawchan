package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.data.network.proxy.ProxyRoute
import com.solthariel.pawchan.data.network.proxy.ScopedProxySelector
import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.proxy.ProxyService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class MirrorProbeProxyTest {
    @Test fun unavailableProxyPreventsDirectMirrorProbe() {
        val router = ScopedProxySelector()
        router.update(ProxyService.DVACH, ProxyRoute.Unavailable)
        val latch = CountDownLatch(1)
        val result = AtomicReference<MirrorAvailability>()
        MirrorProbe(router).use { probe ->
            probe.start("2ch.su") { observation ->
                result.set(observation.availability)
                latch.countDown()
            }
            assertTrue(latch.await(6, TimeUnit.SECONDS))
            assertEquals(MirrorAvailability.NETWORK_ERROR, result.get())
        }
    }
}
