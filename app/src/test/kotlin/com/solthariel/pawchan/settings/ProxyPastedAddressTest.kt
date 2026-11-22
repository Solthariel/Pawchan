package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProxyPastedAddressTest {
    private fun editor(): ProxyEditorState {
        val proxy = SavedProxy("id", "proxy", ProxyMode.HTTP, "", true, setOf(ProxyService.DVACH))
        return ProxyEditorState(proxy)
    }

    @Test fun pasteHostAndPortSplitsAndReplacesOldPort() {
        val state = editor()
        state.port = "3128"
        state.changeHost("  proxy.example:1080  ")
        assertEquals("proxy.example", state.host)
        assertEquals("1080", state.port)
        assertEquals("proxy.example:1080", state.current.address)
    }

    @Test fun ipv4AndBracketedIpv6AreSplit() {
        val state = editor()
        state.changeHost("192.0.2.1:8080")
        assertEquals("192.0.2.1", state.host)
        assertEquals("8080", state.port)
        state.changeHost("[2001:db8::1]:1080")
        assertEquals("2001:db8::1", state.host)
        assertEquals("1080", state.port)
        assertEquals("[2001:db8::1]:1080", state.current.address)
    }

    @Test fun invalidEndpointIsNotMadeValidBySplitting() {
        val state = editor()
        state.changeHost("123.321.213.321:1080")
        assertEquals("123.321.213.321", state.host)
        assertEquals("1080", state.port)
        assertEquals("", state.current.address)
        assertFalse(validDraft(state.current))
    }

    @Test fun ambiguousOrMalformedInputsStayUntouched() {
        listOf("2001:db8::1", "user:password@host:1080", "http://host:1080", "host:abc", "[::1]", "host:")
            .forEach { value ->
                val state = editor()
                state.port = "9000"
                state.changeHost(value)
                assertEquals(value, state.host)
                assertEquals("9000", state.port)
            }
    }

    @Test fun ordinaryTypingDoesNotSplitPartialIpv6() {
        val state = editor()
        listOf("2", "20", "200", "2001", "2001:", "2001:1", "2001:1:").forEach(state::changeHost)
        assertEquals("2001:1:", state.host)
        assertEquals("", state.port)
    }

    @Test fun pastingPortAfterExistingHostAlsoWorks() {
        val state = editor()
        state.host = "example.org"
        state.changeHost("example.org:1234")
        assertEquals("example.org", state.host)
        assertEquals("1234", state.port)
    }
}
