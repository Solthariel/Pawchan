package com.solthariel.pawchan.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.activeContentFilters
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.settings.DEFAULT_SHOW_HIDDEN_POSTS
import com.solthariel.pawchan.domain.thread.ThreadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class PostPreviewState(
    val content: ThreadUiState.Content? = null,
    val posts: List<ThreadPostUi> = emptyList(),
    val failure: String? = null,
    val remote: Boolean = false,
    val replies: Boolean = false,
)

internal fun postPreviewIsMissing(
    state: PostPreviewState,
    target: PostPreviewTarget,
): Boolean = state.failure == null && state.content != null && state.posts.isEmpty() && !target.replies

internal class PostPreviewViewModel(
    private val repository: ThreadRepository?,
    rules: Flow<List<ContentFilterRule>> = flowOf(emptyList()),
    initial: PostPreviewState = PostPreviewState(),
    private val presentationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    showHiddenPosts: Flow<Boolean> = flowOf(DEFAULT_SHOW_HIDDEN_POSTS),
) : ViewModel() {
    private val mutableState = MutableStateFlow(initial)
    private var job: Job? = null
    private var loadedTarget: PostPreviewTarget? = null
    private var loadedLocal: ThreadUiState.Content? = null
    val state =
        combine(mutableState, rules.activeContentFilters(), showHiddenPosts) { raw, filters, placeholders ->
            raw.withPreviewFilters(filters, placeholders)
        }.flowOn(presentationDispatcher).stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    fun load(
        target: PostPreviewTarget,
        local: ThreadUiState.Content?,
    ) {
        val source = local?.takeIf { target.address == null || target.address == it.address }
        if (loadedTarget == target && loadedLocal === source && mutableState.value.content != null) return
        job?.cancel()
        if (loadedTarget != target) mutableState.value = localPostPreview(target, source) ?: PostPreviewState()
        loadedTarget = target
        loadedLocal = source
        localPostPreview(target, source)?.let {
            mutableState.value = retainPreview(mutableState.value, it)
            return
        }
        job =
            viewModelScope.launch {
                val replacement = prepare(target, local)
                currentCoroutineContext().ensureActive()
                mutableState.value = retainPreview(mutableState.value, replacement)
            }
    }

    private suspend fun prepare(
        target: PostPreviewTarget,
        local: ThreadUiState.Content?,
    ): PostPreviewState {
        val address = target.address
        val matches = local != null && (address == null || address == local.address)
        val localPost = local?.posts?.any { it.id == target.postId } == true
        val content = if (matches && (target.replies || localPost)) local else loadRemote(address)
        return withContext(presentationDispatcher) {
            val posts =
                if (target.replies) {
                    content
                        ?.presentation
                        ?.replyIndex
                        ?.get(target.postId)
                        ?.posts
                        .orEmpty()
                } else {
                    content
                        ?.posts
                        ?.firstOrNull { it.id == target.postId }
                        ?.let(::listOf)
                        .orEmpty()
                }
            PostPreviewState(
                content,
                posts,
                if (content == null) "Не удалось загрузить пост" else null,
                remote = !(matches && (target.replies || localPost)),
                replies = target.replies,
            )
        }
    }

    fun retry(
        target: PostPreviewTarget,
        local: ThreadUiState.Content?,
    ) {
        loadedLocal = null
        loadedTarget = null
        load(target, local)
    }

    fun cancelLoading() {
        job?.cancel()
    }

    private suspend fun loadRemote(address: ThreadAddress?): ThreadUiState.Content? {
        if (address == null || repository == null) return null
        val outcome = repository.loadThread(address)
        currentCoroutineContext().ensureActive()
        return withContext(presentationDispatcher) { outcome.toThreadUiState() as? ThreadUiState.Content }
    }

    companion object {
        fun factory(
            repository: ThreadRepository?,
            rules: Flow<List<ContentFilterRule>> = flowOf(emptyList()),
            target: PostPreviewTarget? = null,
            local: ThreadUiState.Content? = null,
            presentationDispatcher: CoroutineDispatcher = Dispatchers.Default,
            showHiddenPosts: Flow<Boolean> = flowOf(DEFAULT_SHOW_HIDDEN_POSTS),
        ): ViewModelProvider.Factory {
            val initial = target?.let { localPostPreview(it, local) } ?: PostPreviewState()
            return viewModelFactory {
                initializer {
                    PostPreviewViewModel(repository, rules, initial, presentationDispatcher, showHiddenPosts)
                }
            }
        }
    }
}
