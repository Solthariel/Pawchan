package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.data.network.ArhivachBrowser
import com.solthariel.pawchan.data.network.FourChanReadApi
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.proxy.ProxyService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyClientIntegrationTest {
    @Test fun archiveAndFourChanFailWhenTheirProxyIsUnavailable() =
        runBlocking {
            val router = ScopedProxySelector()
            router.update(ProxyService.ARCHIVE, ProxyRoute.Unavailable)
            router.update(ProxyService.FOUR_CHAN, ProxyRoute.Unavailable)
            ArhivachBrowser.create(router = router).use { browser ->
                assertTrue(browser.loadThreads(emptyList(), 0) is DomainOutcome.Failure)
            }
            FourChanReadApi.create(router = router).use { api ->
                assertTrue(api.loadBoards(true) is DomainOutcome.Failure)
            }
        }
}
