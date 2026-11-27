package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.infrastructure.CookieRuleStore
import com.solthariel.pawchan.infrastructure.CookieRules
import com.solthariel.pawchan.infrastructure.SiteCookies
import com.solthariel.pawchan.settings.CookiesController
import com.solthariel.pawchan.settings.CookiesPage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** «Сеть → Куки»: скрытые значения, блокировка, удаление при выходе и «Очистить все» с подтверждением. */
class CookiesRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private val site = FakeSiteCookies(mutableListOf("usercode_auth" to "secret123", "cf_clearance" to "token"))
    private val store = CookieRuleStore(CookieRules()) {}

    @Test
    fun valuesAreMaskedUntilTheEyeIsTapped() {
        show()
        compose.onNodeWithText("secret123", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Показать значение: usercode_auth").performClick()
        compose.onNodeWithText("secret123", substring = true).assertExists()
    }

    @Test
    fun blockingDeletesTheCookieAndKeepsItListed() {
        show()
        compose.onNodeWithTag("cookie-usercode_auth").performClick()
        compose.onNodeWithText("Блокировать").performClick()
        compose.runOnIdle {
            assertEquals(setOf("usercode_auth"), store.current().blocked)
            assertEquals(listOf("cf_clearance"), site.list().map { it.first })
        }
        compose.onNodeWithText("Блокируется", substring = true).assertExists()
    }

    @Test
    fun deleteOnExitIsRemembered() {
        show()
        compose.onNodeWithTag("cookie-cf_clearance").performClick()
        compose.onNodeWithText("Удалять при выходе").performClick()
        compose.runOnIdle { assertEquals(setOf("cf_clearance"), store.current().deleteOnExit) }
        compose.onNodeWithText("Удаляется при выходе", substring = true).assertExists()
    }

    @Test
    fun clearAllAsksFirst() {
        show()
        compose.onNodeWithContentDescription("Очистить все").performClick()
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle { assertEquals(2, site.list().size) }
        compose.onNodeWithContentDescription("Очистить все").performClick()
        compose.onNodeWithText("Очистить все").performClick()
        compose.runOnIdle { assertEquals(0, site.list().size) }
        compose.onNodeWithText("Куки нет").assertExists()
    }

    private fun show() {
        compose.setContent { MaterialTheme { CookiesPage(CookiesController(site, store)) {} } }
        compose.waitForIdle()
    }

    private class FakeSiteCookies(
        private val cookies: MutableList<Pair<String, String>>,
    ) : SiteCookies {
        override val host = "2ch.su"

        override fun list(): List<Pair<String, String>> = cookies.toList()

        override fun delete(name: String) {
            cookies.removeAll { it.first == name }
        }
    }
}
