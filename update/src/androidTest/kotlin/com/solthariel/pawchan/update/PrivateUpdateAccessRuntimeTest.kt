package com.solthariel.pawchan.update

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.File

class PrivateUpdateAccessRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val token = "github_pat_instrumentation_test_value_only"

    @Test fun keystoreEncryptedAccessSurvivesStoreRecreationWithoutPlaintext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidUpdateTokenStore(context)
        try {
            store.save(token)
            assertEquals(token, AndroidUpdateTokenStore(context).read())
            val bytes = File(context.noBackupFilesDir, "private-update-access.bin").readBytes()
            assertFalse(String(bytes, Charsets.ISO_8859_1).contains(token))
        } finally {
            store.clear()
        }
    }

    @Test fun clearingAccessRemovesItForANewStoreInstance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidUpdateTokenStore(context)
        try {
            store.save(token)
            store.clear()
            assertNull(AndroidUpdateTokenStore(context).read())
        } finally {
            store.clear()
        }
    }

    @Test fun tokenIsSavedOnlyAfterExplicitActionAndRemovedByTheUser() {
        val store = MemoryUpdateTokenStore()
        val changes = mutableListOf<Boolean>()
        compose.setContent { MaterialTheme { PrivateUpdateAccess(store) { changes += it } } }
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText("GitHub token — Contents: Read").assertIsEnabled() }.isSuccess
        }
        compose.onNodeWithText("GitHub token — Contents: Read").performTextInput(token)
        compose.runOnIdle { assertNull(store.read()) }
        compose.onNodeWithText("Сохранить доступ").performClick()
        compose.waitUntil(5_000) { changes == listOf(true) }
        compose.onNodeWithText("Доступ сохранён на этом устройстве").assertIsDisplayed()
        compose.onNodeWithText("Удалить доступ").performClick()
        compose.waitUntil(5_000) { changes == listOf(true, false) }
        compose.runOnIdle { assertNull(store.read()) }
    }

    @Test fun invalidInputDoesNotSaveOrInvokeUpdateCheck() {
        val store = MemoryUpdateTokenStore()
        var changes = 0
        compose.setContent { MaterialTheme { PrivateUpdateAccess(store) { changes++ } } }
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText("GitHub token — Contents: Read").assertIsEnabled() }.isSuccess
        }
        compose.onNodeWithText("GitHub token — Contents: Read").performTextInput("short")
        compose.onNodeWithText("Сохранить доступ").performClick()
        compose.runOnIdle {
            assertNull(store.read())
            assertEquals(0, changes)
        }
    }
}

private class MemoryUpdateTokenStore : UpdateTokenStore {
    @Volatile private var token: String? = null

    override fun read(): String? = token

    override fun save(token: String) {
        this.token = token
    }

    override fun clear() {
        token = null
    }
}
