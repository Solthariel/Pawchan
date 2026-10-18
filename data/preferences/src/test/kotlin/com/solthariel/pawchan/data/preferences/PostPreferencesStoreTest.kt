package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.postpreferences.PostPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PostPreferencesStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("sp")))
    private val address = ThreadAddress(board, requireNotNull(ThreadId.parse(42)))

    @Test
    fun rulesSurviveReopeningAndDoNotLeakToOtherThreads() =
        runBlocking {
            val file = File(temporary.root, "settings.preferences_pb")
            val expected =
                PostPreferences(
                    own = setOf(1),
                    hidden = setOf(2),
                    replyTrees = setOf(3),
                    names = setOf("имя"),
                    similar = setOf("текст\nмного строк"),
                    shown = setOf(4),
                )
            val firstJob = SupervisorJob()
            val first =
                PostPreferencesStore(
                    PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO)) { file },
                )
            assertEquals(DomainOutcome.Success(Unit), first.savePostPreferences(address, expected))
            firstJob.cancelAndJoin()
            val nextJob = SupervisorJob()
            try {
                val restored =
                    PostPreferencesStore(
                        PreferenceDataStoreFactory.create(scope = CoroutineScope(nextJob + Dispatchers.IO)) { file },
                    )
                assertEquals(DomainOutcome.Success(expected), restored.loadPostPreferences(address))
                assertEquals(
                    DomainOutcome.Success(PostPreferences()),
                    restored.loadPostPreferences(address.copy(thread = requireNotNull(ThreadId.parse(43)))),
                )
            } finally {
                nextJob.cancelAndJoin()
            }
        }
}
