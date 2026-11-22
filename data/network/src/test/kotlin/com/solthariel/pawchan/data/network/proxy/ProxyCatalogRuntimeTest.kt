package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyPreferences
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
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
class ProxyCatalogRuntimeTest {
    private val one =
        SavedProxy(
            "one",
            "Мой",
            ProxyMode.SOCKS5,
            "127.0.0.1:1080",
            true,
            setOf(ProxyService.ARCHIVE, ProxyService.GITHUB),
        )

    @Test fun oneProxyCanRouteMultipleServicesOnlyAfterAtomicSave() =
        runTest {
            val router = ScopedProxySelector(null)
            val services = ServiceProxyRuntime(router, ProxyPreferences(), backgroundScope) { _, _ -> }
            val saved = mutableListOf<List<SavedProxy>>()
            val catalog = ProxyCatalogRuntime(services, ProxyPreferences(), backgroundScope) { saved.add(it) }
            assertTrue(catalog.save(listOf(one)))
            assertEquals(ProxyRoute.Unavailable, router.route(ProxyService.ARCHIVE))
            testScheduler.runCurrent()
            val route = ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1080))
            assertEquals(route, router.route(ProxyService.ARCHIVE))
            assertEquals(route, router.route(ProxyService.GITHUB))
            assertEquals(ProxyRoute.Direct, router.route(ProxyService.VOCAROO))
            assertEquals(listOf(listOf(one)), saved)
        }

    @Test fun conflictingEnabledAssignmentsAreRejected() =
        runTest {
            val router = ScopedProxySelector(null)
            val services = ServiceProxyRuntime(router, ProxyPreferences(), backgroundScope) { _, _ -> }
            val catalog = ProxyCatalogRuntime(services, ProxyPreferences(), backgroundScope) { }
            assertFalse(catalog.save(listOf(one, one.copy(id = "two"))))
            assertEquals(ProxyRoute.Direct, router.route(ProxyService.GITHUB))
            assertTrue(catalog.save(listOf(one, one.copy(id = "two", enabled = false))))
        }

    @Test fun failedSaveKeepsAffectedRoutesBlocked() =
        runTest {
            val router = ScopedProxySelector(null)
            val services = ServiceProxyRuntime(router, ProxyPreferences(), backgroundScope) { _, _ -> }
            val catalog =
                ProxyCatalogRuntime(services, ProxyPreferences(), backgroundScope) {
                    throw IOException("disk")
                }
            assertTrue(catalog.save(listOf(one)))
            testScheduler.runCurrent()
            assertTrue(catalog.state.value.saveFailed)
            assertEquals(ProxyRoute.Unavailable, router.route(ProxyService.ARCHIVE))
            assertEquals(ProxyRoute.Unavailable, router.route(ProxyService.GITHUB))
            assertEquals(ProxyRoute.Direct, router.route(ProxyService.VOCAROO))
        }

    @Test fun checkDoesNotContactUnselectedResourceOrUseAnInvalidRoute() =
        runTest {
            val router = ScopedProxySelector(null)
            val services = ServiceProxyRuntime(router, ProxyPreferences(), backgroundScope) { _, _ -> }
            val catalog = ProxyCatalogRuntime(services, ProxyPreferences(), backgroundScope) { }
            assertEquals(ProxyProbeResult.Unavailable, catalog.check(one, ProxyService.VOCAROO, "2ch.su"))
            assertEquals(
                ProxyProbeResult.Unavailable,
                catalog.check(one.copy(address = "bad"), ProxyService.ARCHIVE, "2ch.su"),
            )
            assertEquals(ProxyRoute.Direct, router.route(ProxyService.ARCHIVE))
        }

    @Test fun latestSaveWinsEvenWhenOlderWriteCannotBeCancelled() =
        runTest {
            val router = ScopedProxySelector(null)
            val services = ServiceProxyRuntime(router, ProxyPreferences(), backgroundScope) { _, _ -> }
            val saved = mutableListOf<List<SavedProxy>>()
            val catalog =
                ProxyCatalogRuntime(services, ProxyPreferences(), backgroundScope) {
                    withContext(NonCancellable) {
                        delay(100)
                        saved.add(it)
                    }
                }
            catalog.save(listOf(one))
            testScheduler.runCurrent()
            catalog.save(emptyList())
            testScheduler.advanceTimeBy(250)
            testScheduler.runCurrent()
            assertEquals(listOf(listOf(one), emptyList<SavedProxy>()), saved)
            assertEquals(ProxyRoute.Direct, router.route(ProxyService.ARCHIVE))
        }
}
