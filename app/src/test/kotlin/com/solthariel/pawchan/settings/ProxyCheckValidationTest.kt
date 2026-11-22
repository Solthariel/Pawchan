package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyCheckValidationTest {
    private val draft = SavedProxy("id", "", ProxyMode.HTTP, "localhost:8080", true, setOf(ProxyService.ARCHIVE))

    @Test fun checkRequiresAddressResourcesAndValidCredentialsButNotProfileName() {
        assertTrue(canCheckProxy(draft))
        assertFalse(canCheckProxy(draft.copy(address = "")))
        assertFalse(canCheckProxy(draft.copy(services = emptySet())))
        assertFalse(canCheckProxy(draft.copy(credentials = ProxyCredentials("user", ""))))
        assertFalse(canCheckProxy(draft.copy(credentials = ProxyCredentials("bad:name", "pass"))))
        assertTrue(canCheckProxy(draft.copy(credentials = ProxyCredentials("user", "pass"))))
    }
}
