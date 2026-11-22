package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyProfile
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProxyPreferencesStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun catalogAndAssignmentsPersistAcrossReopen() =
        runBlocking {
            val file = File(temporary.root, "proxy.preferences_pb")
            val job = SupervisorJob()
            val firstStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
            val first = ProxyPreferencesStore(firstStore, TestSealer)
            val proxy =
                SavedProxy(
                    "one",
                    "Мой прокси",
                    ProxyMode.HTTP,
                    "http://1.1.1.1:8080",
                    true,
                    setOf(ProxyService.ARCHIVE, ProxyService.GITHUB),
                    ProxyCredentials("user", "secret"),
                )
            first.saveCatalog(listOf(proxy))
            job.cancelAndJoin()
            val nextJob = SupervisorJob()
            try {
                val nextStore =
                    PreferenceDataStoreFactory.create(scope = CoroutineScope(nextJob + Dispatchers.IO)) { file }
                val next = ProxyPreferencesStore(nextStore, TestSealer)
                val saved = next.preferences.first()
                assertEquals(listOf(proxy), saved.savedProxies)
                assertEquals(
                    ProxyProfile(ProxyMode.HTTP, proxy.address, proxy.credentials),
                    saved.profile(ProxyService.ARCHIVE),
                )
                assertEquals(saved.profile(ProxyService.ARCHIVE), saved.profile(ProxyService.GITHUB))
                assertEquals(ProxyMode.DIRECT, saved.profile(ProxyService.VOCAROO).mode)
            } finally {
                nextJob.cancelAndJoin()
            }
        }
}

private object TestSealer : ProxySecretSealer {
    override fun seal(plaintext: ByteArray): ByteArray = plaintext.map { (it.toInt() xor 0x5a).toByte() }.toByteArray()

    override fun open(sealed: ByteArray): ByteArray = seal(sealed)
}
