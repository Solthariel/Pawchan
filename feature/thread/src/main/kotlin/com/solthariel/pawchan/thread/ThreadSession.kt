package com.solthariel.pawchan.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.thread.watch.ThreadWatcherController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/** ViewModelStore-owned session. UI state and controllers never leave the feature. */
abstract class ThreadSession internal constructor() : ViewModel() {
    abstract fun refresh()

    abstract fun refreshFavorite()

    abstract fun onPostSubmitted(post: BoardPost)

    internal abstract val state: StateFlow<ThreadUiState>
    internal abstract val watcher: ThreadWatcherController
    internal abstract val postPreferences: ThreadPostPreferencesController
    internal abstract val reports: ThreadReportController

    internal open fun revealContentFilter(id: Long) = Unit

    internal abstract val sessionAddress: ThreadAddress

    internal abstract fun setFavorite(favorite: Boolean)

    /** Экран снова показан (повторный вход в удержанную сессию или возврат в приложение). */
    internal abstract fun reopen()

    internal abstract fun toggleWatcher()

    /** null — история треда не сохраняется, пунктов «Стереть»/«Очистить» нет. */
    internal abstract val history: ThreadHistoryActions?

    companion object {
        fun factory(
            address: ThreadAddress,
            repository: ThreadRepository,
            persistence: ThreadPersistence,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer<ThreadSession> {
                    ThreadViewModel(
                        address,
                        repository,
                        persistence,
                        ThreadViewModelEnvironment(uiDispatcher = Dispatchers.Default),
                    )
                }
            }
    }
}
