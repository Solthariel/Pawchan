package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.AppBehavior
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.DnsProvider
import com.solthariel.pawchan.domain.settings.ServiceDns
import com.solthariel.pawchan.domain.settings.ServiceDnsPreferences
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

class AppSettingsStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun defaultsUntilChangedAndChangesSurviveReopening() =
        runBlocking {
            val file = File(temporary.root, "settings.preferences_pb")
            val firstJob = SupervisorJob()
            val first =
                AppSettingsStore(
                    PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO)) { file },
                )
            assertEquals(AppSettings(), first.appSettings.first())
            val outcome =
                first.updateAppSettings { it.copy(textSize = 99) }
            assertEquals(DomainOutcome.Success(Unit), outcome)
            firstJob.cancelAndJoin()
            val nextJob = SupervisorJob()
            try {
                val restored =
                    AppSettingsStore(
                        PreferenceDataStoreFactory.create(scope = CoroutineScope(nextJob + Dispatchers.IO)) { file },
                    ).appSettings.first()
                assertEquals(AppSettings.MAX_TEXT_SIZE, restored.textSize)
                // Размер текста ограничен допустимым диапазоном.
                assertEquals(AppSettings.MAX_TEXT_SIZE, restored.textSize)
            } finally {
                nextJob.cancelAndJoin()
            }
        }

    @Test
    fun contentRulesAndOtherBehaviorSurviveReopening() =
        runBlocking {
            val file = File(temporary.root, "filters.preferences_pb")
            val rules =
                listOf(
                    ContentFilterRule("rule", "dvach", condition = ContentFilterConditionSpec(values = listOf("OP"))),
                )
            val expected =
                AppSettings(
                    bubblePosts = false,
                    hideStickers = true,
                    behavior =
                        AppBehavior(
                            postCountEnabled = false,
                            videoStartMuted = true,
                            dns =
                                ServiceDnsPreferences(
                                    vocaroo = ServiceDns(DnsProvider.XBOX),
                                    arhivach = ServiceDns(DnsProvider.CUSTOM, "https://archive.example/dns-query"),
                                ),
                            updateSavedCopies = false,
                            showHiddenThreads = true,
                            client =
                                com.solthariel.pawchan.domain.settings.ClientPreferences(
                                    media =
                                        com.solthariel.pawchan.domain.settings.MediaPreferences(
                                            playAfterScroll = false,
                                            syncThreadPosition = true,
                                            clearCacheOnExit = true,
                                        ),
                                ),
                        ),
                ).withContentFilters(rules)
            val firstJob = SupervisorJob()
            try {
                val first =
                    AppSettingsStore(
                        PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO)) { file },
                    )
                assertEquals(DomainOutcome.Success(Unit), first.updateAppSettings { expected })
            } finally {
                firstJob.cancelAndJoin()
            }
            val nextJob = SupervisorJob()
            try {
                val restored =
                    AppSettingsStore(
                        PreferenceDataStoreFactory.create(scope = CoroutineScope(nextJob + Dispatchers.IO)) { file },
                    ).appSettings.first()
                assertEquals(expected, restored)
                assertEquals(rules, restored.contentFilters)
            } finally {
                nextJob.cancelAndJoin()
            }
        }
}
