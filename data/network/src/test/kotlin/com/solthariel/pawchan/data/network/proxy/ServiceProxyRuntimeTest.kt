package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyPreferences
import com.solthariel.pawchan.domain.proxy.ProxyProfile
import com.solthariel.pawchan.domain.proxy.ProxyService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceProxyRuntimeTest {
    private val site = ProxyService.ARCHIVE
    private val blocked = ProxyProfile(ProxyMode.NEEDS_CONFIGURATION)
    private val manual = ProxyProfile(ProxyMode.SOCKS5, "127.0.0.1:1080")

    @Test fun unconfiguredSettingsRemainBlockedUntilExplicitChoice() =
        runTest {
            val router = ScopedProxySelector()
            val runtime =
                ServiceProxyRuntime(router, ProxyPreferences(mapOf(site to blocked)), backgroundScope) { _, _ -> }
            assertEquals(ProxyRoute.Unavailable, router.route(site))
            testScheduler.runCurrent()
            assertEquals(
                ProxyRuntimeStatus.NEEDS_CONFIGURATION,
                runtime.states.value
                    .getValue(site)
                    .status,
            )
            assertEquals(ProxyRoute.Unavailable, router.route(site))
            assertFalse(runtime.setProfile(site, ProxyProfile(ProxyMode.SOCKS5, "bad")))
            assertEquals(ProxyRoute.Unavailable, router.route(site))
            assertTrue(runtime.setProfile(site, manual))
            assertEquals(ProxyRoute.Unavailable, router.route(site))
            testScheduler.runCurrent()
            assertEquals(ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1080)), router.route(site))
            assertEquals(ProxyRoute.Direct, router.route(ProxyService.GITHUB))
            assertEquals(
                ProxyRuntimeStatus.READY,
                runtime.states.value
                    .getValue(site)
                    .status,
            )
        }

    @Test fun httpProxyRemainsBlockedUntilSavedThenRoutesOnlySelectedService() =
        runTest {
            val router = ScopedProxySelector(null)
            val runtime = ServiceProxyRuntime(router, ProxyPreferences(), backgroundScope) { _, _ -> }
            assertFalse(runtime.setProfile(site, ProxyProfile(ProxyMode.HTTP, "https://127.0.0.1:8080")))
            assertTrue(runtime.setProfile(site, ProxyProfile(ProxyMode.HTTP, "http://127.0.0.1:8080")))
            assertEquals(ProxyRoute.Unavailable, router.route(site))
            testScheduler.runCurrent()
            assertEquals(ProxyRoute.Http(ProxyEndpoint("127.0.0.1", 8080)), router.route(site))
            assertEquals(ProxyRoute.Direct, router.route(ProxyService.GITHUB))
        }

    @Test fun failedSaveDoesNotEnableDirectOrManual() =
        runTest {
            val router = ScopedProxySelector()
            val runtime =
                ServiceProxyRuntime(router, ProxyPreferences(mapOf(site to blocked)), backgroundScope) { _, _ ->
                    throw IOException("disk")
                }
            assertTrue(runtime.setProfile(site, ProxyProfile()))
            testScheduler.runCurrent()
            assertEquals(
                ProxyRuntimeStatus.SAVE_FAILED,
                runtime.states.value
                    .getValue(site)
                    .status,
            )
            assertEquals(ProxyRoute.Unavailable, router.route(site))
            assertTrue(runtime.setProfile(site, manual))
            testScheduler.runCurrent()
            assertEquals(
                ProxyRuntimeStatus.SAVE_FAILED,
                runtime.states.value
                    .getValue(site)
                    .status,
            )
            assertEquals(ProxyRoute.Unavailable, router.route(site))
        }

    @Test fun olderSlowSaveCannotRestoreAnObsoleteRoute() =
        runTest {
            val router = ScopedProxySelector()
            val saved = mutableListOf<ProxyProfile>()
            val runtime =
                ServiceProxyRuntime(router, ProxyPreferences(), backgroundScope) { _, profile ->
                    withContext(NonCancellable) {
                        delay(100)
                        saved.add(profile)
                    }
                }
            assertTrue(runtime.setProfile(site, manual))
            testScheduler.runCurrent()
            assertTrue(runtime.setProfile(site, ProxyProfile()))
            assertEquals(ProxyRoute.Unavailable, router.route(site))
            testScheduler.advanceTimeBy(250)
            testScheduler.runCurrent()
            assertEquals(listOf(manual, ProxyProfile()), saved)
            assertEquals(ProxyRoute.Direct, router.route(site))
            assertEquals(
                ProxyRuntimeStatus.DIRECT,
                runtime.states.value
                    .getValue(site)
                    .status,
            )
        }

    @Test fun savedManualAndInvalidAddressesAreAppliedIndependently() =
        runTest {
            val router = ScopedProxySelector()
            val profiles =
                ProxyPreferences(
                    mapOf(site to manual, ProxyService.GITHUB to ProxyProfile(ProxyMode.SOCKS5, "bad")),
                )
            val runtime = ServiceProxyRuntime(router, profiles, backgroundScope) { _, _ -> }
            assertEquals(ProxyRoute.Unavailable, router.route(site))
            assertEquals(ProxyRoute.Unavailable, router.route(ProxyService.GITHUB))
            testScheduler.runCurrent()
            assertEquals(ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1080)), router.route(site))
            assertEquals(
                ProxyRuntimeStatus.INVALID_ADDRESS,
                runtime.states.value
                    .getValue(ProxyService.GITHUB)
                    .status,
            )
            assertEquals(ProxyRoute.Unavailable, router.route(ProxyService.GITHUB))
        }
}
