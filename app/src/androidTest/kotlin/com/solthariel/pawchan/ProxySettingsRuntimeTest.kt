package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.solthariel.pawchan.data.network.proxy.ProxyCatalogRuntime
import com.solthariel.pawchan.data.network.proxy.ProxyEndpoint
import com.solthariel.pawchan.data.network.proxy.ProxyRoute
import com.solthariel.pawchan.data.network.proxy.ScopedProxySelector
import com.solthariel.pawchan.data.network.proxy.ServiceProxyRuntime
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyPreferences
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import com.solthariel.pawchan.domain.proxy.proxyPreferencesFromCatalog
import com.solthariel.pawchan.infrastructure.LocalProxyCatalog
import com.solthariel.pawchan.settings.ProxyCatalogSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProxySettingsRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @After fun cleanup() {
        scope.cancel()
    }

    @Test fun togglingDoesNotMoveRowsWhileSaveIsPending() {
        val proxy = SavedProxy("id", "Мой прокси", ProxyMode.SOCKS5, "localhost:1080", false, setOf(ProxyService.ARCHIVE))
        val initial = proxyPreferencesFromCatalog(listOf(proxy))
        val gate = CompletableDeferred<Unit>()
        val services = ServiceProxyRuntime(ScopedProxySelector(null), initial, scope) { _, _ -> }
        val catalog = ProxyCatalogRuntime(services, initial, scope) { gate.await() }
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalProxyCatalog provides catalog) { ProxyCatalogSettings("2ch.su") {} }
            }
        }
        val before = compose.onNodeWithText("Мой прокси").fetchSemanticsNode().boundsInRoot
        compose.onNode(isToggleable()).performClick()
        compose.waitUntil(3000) { catalog.state.value.saving }
        assertEquals(before, compose.onNodeWithText("Мой прокси").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithText("Сохранение…").assertDoesNotExist()
        gate.complete(Unit)
    }

    @Test fun addOneProxyForTwoResources() {
        val router = ScopedProxySelector(null)
        val services = ServiceProxyRuntime(router, ProxyPreferences(), scope) { _, _ -> }
        val catalog = ProxyCatalogRuntime(services, ProxyPreferences(), scope) { }
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalProxyCatalog provides catalog) {
                    ProxyCatalogSettings("2ch.su") { }
                }
            }
        }
        compose.onNodeWithContentDescription("Добавить прокси").performClick()
        compose.onNodeWithText("Название").performTextInput("Тестовый")
        compose.onNodeWithText("Адрес").performTextInput("127.0.0.1")
        compose.onNodeWithText("Порт").performTextInput("1080")
        compose.onNodeWithContentDescription("Ресурс Архивач").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ресурс GitHub").performScrollTo().performClick()
        compose.onNodeWithText("Проверить выбранные").performScrollTo().assertIsEnabled()
        compose.onNodeWithContentDescription("Сохранить").performClick()
        val expected = ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1080))
        compose.waitUntil(3000) {
            router.route(ProxyService.ARCHIVE) == expected && router.route(ProxyService.GITHUB) == expected
        }
        compose.onNodeWithText("Тестовый").assertExists()
    }
}
