package com.solthariel.pawchan

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.solthariel.pawchan.data.preferences.PreferencesRecentBoardsRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentBoardsRuntimeTest {
    @Test
    fun historySurvivesRepositoryRecreationAndMovesVisitedBoardFirst() =
        runBlocking {
            val context = HistoryTestContext()
            context.deleteSharedPreferences("test-board-lookup-recent-boards")
            try {
                val first = defaultBoardAddress()
                val second = BoardAddress(first.site, requireNotNull(BoardId.parse("mobi")))
                PreferencesRecentBoardsRepository(context).visit(first)
                PreferencesRecentBoardsRepository(context).visit(second)
                val result = PreferencesRecentBoardsRepository(context).visit(first)
                assertEquals(DomainOutcome.Success(listOf(first, second)), result)
            } finally {
                context.deleteSharedPreferences("test-board-lookup-recent-boards")
            }
        }

    @Test
    fun deniedStorageProducesDomainFailure() =
        runBlocking {
            val context =
                object : HistoryTestContext() {
                    override fun getSharedPreferences(
                        name: String,
                        mode: Int,
                    ): SharedPreferences = throw SecurityException("Test storage is unavailable")
                }
            val result = PreferencesRecentBoardsRepository(context).visit(defaultBoardAddress())
            assertEquals(DomainOutcome.Failure(DomainFailure.StorageUnavailable), result)
        }

    private open class HistoryTestContext : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(
            name: String,
            mode: Int,
        ): SharedPreferences = super.getSharedPreferences("test-board-lookup-$name", mode)
    }
}
