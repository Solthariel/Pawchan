package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.postpreferences.PostPreferences
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class ThreadPostPreferencesController(
    private val address: ThreadAddress,
    private val repository: PostPreferencesRepository?,
    private val state: PostContentAccess,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val mutex = Mutex()
    private var rules: PostPreferences? = null
        set(value) {
            field = value
            publishedRules.value = value
        }
    private val publishedRules = MutableStateFlow<PostPreferences?>(null)

    /** Текущие правила скрытия — для списка «Скрытые посты». */
    val currentRules: StateFlow<PostPreferences?> = publishedRules.asStateFlow()

    suspend fun prepare(content: ThreadUiState): ThreadUiState =
        mutex.withLock {
            if (rules == null) {
                rules =
                    if (repository == null) {
                        PostPreferences()
                    } else {
                        (repository.loadPostPreferences(address) as? DomainOutcome.Success)?.value
                    }
            }
            decorate(content, rules ?: PostPreferences())
        }

    suspend fun change(
        action: PostPreferenceAction,
        post: ThreadPostUi,
    ): Boolean = update { it.changed(action, post) }

    /** «Скрытые посты»: правило убирается, посты, скрытые только им, снова видны. */
    suspend fun remove(rule: HiddenPostRule): Boolean = update { it.without(rule) }

    private suspend fun update(transform: (PostPreferences) -> PostPreferences): Boolean =
        mutex.withLock {
            val current = rules ?: return@withLock false
            val next = transform(current)
            if (repository?.savePostPreferences(address, next) is DomainOutcome.Failure) return@withLock false
            rules = next
            var updated = false
            while (!updated) {
                val expected = state.read()
                val result = decorate(expected, next)
                if (state.read() === expected) {
                    state.write(result)
                    updated = true
                }
            }
            true
        }

    suspend fun markPosted(id: Long) =
        mutex.withLock {
            val current =
                rules ?: if (repository == null) {
                    PostPreferences()
                } else {
                    (repository.loadPostPreferences(address) as? DomainOutcome.Success)?.value ?: return@withLock
                }
            val next = current.copy(own = current.own + id)
            if (repository?.savePostPreferences(address, next) !is DomainOutcome.Failure) rules = next
        }

    private suspend fun decorate(
        content: ThreadUiState,
        preferences: PostPreferences,
    ): ThreadUiState =
        withContext(dispatcher) {
            if (content !is ThreadUiState.Content) return@withContext content
            val hidden = hiddenPostIds(content.posts, preferences)
            content
                .copy(
                    posts =
                        content.posts.map { post ->
                            val own = post.id in preferences.own
                            post.copy(
                                metadata =
                                    post.metadata.copy(
                                        visibility = post.metadata.visibility.copy(hidden = post.id in hidden),
                                        changes =
                                            post.metadata.changes.copy(
                                                own = own,
                                                replyToMe =
                                                    post.referencedPostIds.any {
                                                        it in
                                                            preferences.own
                                                    },
                                            ),
                                    ),
                            )
                        },
                ).withPostNavigation()
        }
}

internal data class PostContentAccess(
    val read: () -> ThreadUiState,
    val write: (ThreadUiState) -> Unit,
)
