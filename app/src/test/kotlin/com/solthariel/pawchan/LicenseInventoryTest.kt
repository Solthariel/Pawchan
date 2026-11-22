package com.solthariel.pawchan

import com.solthariel.pawchan.settings.DeclaredLicense
import com.solthariel.pawchan.settings.LicensedComponent
import com.solthariel.pawchan.settings.parseLicenseInventory
import org.junit.Assert.assertEquals
import org.junit.Test

class LicenseInventoryTest {
    @Test fun parsesDeclarationsButNotEmbeddedNoticeBodies() {
        val lines =
            listOf(
                "Pawchan: resolved release third-party notices",
                "a.group:first:1.2.3",
                "Licensing source: resolved Gradle POM",
                "Apache-2.0 — https://example.test/license",
                "--- first.aar / LICENSE.txt ---",
                "not:a:coordinate",
                "ordinary copyright text",
                "",
                "org.example:second:2.0-rc1",
                "Licensing source: resolved Gradle POM",
                "MIT — https://example.test/mit",
                "BSD — https://example.test/bsd",
                "",
            )
        assertEquals(
            listOf(
                LicensedComponent(
                    "a.group:first:1.2.3",
                    listOf(DeclaredLicense("Apache-2.0", "https://example.test/license")),
                ),
                LicensedComponent(
                    "org.example:second:2.0-rc1",
                    listOf(
                        DeclaredLicense("MIT", "https://example.test/mit"),
                        DeclaredLicense("BSD", "https://example.test/bsd"),
                    ),
                ),
            ),
            parseLicenseInventory(lines),
        )
    }

    @Test fun ignoresBrokenAndEmptyDeclarations() {
        assertEquals(
            emptyList<LicensedComponent>(),
            parseLicenseInventory(listOf("a:b:1", "Licensing source: local", "bad")),
        )
        assertEquals(emptyList<LicensedComponent>(), parseLicenseInventory(emptyList()))
    }
}
