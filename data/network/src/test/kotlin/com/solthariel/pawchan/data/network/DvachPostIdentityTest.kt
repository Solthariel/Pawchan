package com.solthariel.pawchan.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DvachPostIdentityTest {
    @Test fun recognizesActualHeavenWireIdentityWithEmptyEmail() {
        val identity = decodeDvachPostIdentity("Аноним&nbsp;ID:&nbsp;Heaven", "")
        assertEquals("Аноним", identity.name)
        assertTrue(identity.sage)
    }

    @Test fun supportsHtmlWrappedIdentifier() {
        val identity = decodeDvachPostIdentity("Аноним ID: <span id=\"id_tag_x\">Heaven</span>", null)
        assertEquals("Аноним", identity.name)
        assertTrue(identity.sage)
    }

    @Test fun doesNotGuessSageFromOrdinaryNames() {
        for (name in listOf("Heaven", "Neko Heaven", "Аноним ID: Heavenly", "Аноним ID: heaven", "OP sage")) {
            val identity = decodeDvachPostIdentity(name, null)
            assertEquals(name, identity.name)
            assertFalse(identity.sage)
        }
    }

    @Test fun preservesRegularGeneratedIdentity() {
        val identity = decodeDvachPostIdentity("Аноним ID: <span id=\"id_tag_x\">Веселый Дед Макабка</span>", "")
        assertEquals("Веселый Дед Макабка", identity.name)
        assertFalse(identity.sage)
    }

    @Test fun retainsExplicitEmailSageAliases() {
        for (email in listOf("sage", "mailto:sage", " SAGE ", " MAILTO:SAGE ")) {
            assertTrue(decodeDvachPostIdentity("Neko", email).sage)
        }
        assertFalse(decodeDvachPostIdentity("Neko", "sage@example.com").sage)
    }
}
