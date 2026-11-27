package com.solthariel.pawchan.settings

import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.R
import com.solthariel.pawchan.locale.PawchanLocales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class InterfaceLocalesRuntimeTest {
    @Test fun countsUseTypedArgumentsAndAndroidChoosesTheGrammaticalForm() {
        fun limit(
            tag: String,
            count: Int,
        ): String = resourcesFor(tag).getQuantityString(R.plurals.board_comment_limit, count, count)

        assertEquals("Text limit: 1 character", limit("en", 1))
        assertEquals("Text limit: 2 characters", limit("en", 2))
        assertEquals("Лимит текста: 1 символ", limit("ru", 1))
        assertEquals("Лимит текста: 2 символа", limit("ru", 2))
        assertEquals("Лимит текста: 5 символов", limit("ru", 5))
        assertEquals("Лимит текста: 21 символ", limit("ru", 21))
        val english = resourcesFor("en")
        assertEquals("Inbox, replies: 23", english.getString(R.string.ui_replies_6b38, "Inbox", 23))
        assertEquals("Thread no. 4294967300", english.getString(R.string.ui_thread_no, 4_294_967_300L))
        for (tag in PawchanLocales.tags) {
            assertTrue("Missing limit for $tag", limit(tag, 1_000_000).contains("1000000"))
        }
    }

    @Test fun allTenConfigurationsResolveUiStringsAndUnsupportedLanguageFallsBackToRussian() {
        fun title(tag: String): String = resourcesFor(tag).getString(R.string.interface_language)
        assertEquals("Язык интерфейса", title("ru"))
        assertEquals(title("ru"), title("zz"))
        for (tag in PawchanLocales.tags.filterNot { it == "ru" }) {
            assertTrue("Missing locale $tag", title(tag).isNotBlank() && title(tag) != title("ru"))
        }
        assertEquals("Interface language", title("en"))
    }

    private fun resourcesFor(tag: String): Resources {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(config).resources
    }
}
