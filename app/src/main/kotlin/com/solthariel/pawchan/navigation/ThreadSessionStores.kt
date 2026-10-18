package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.workspace.WorkspaceController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Сессии недавно закрытых «Назад» тредов, как удерживаемые страницы Dashchan (`RetainableExtra`
 * в `MainPageRetainedState`): повторный вход возвращает готовый экран из памяти без пустого кадра
 * загрузки и без пересборки постов. Владелец — Activity; хранилище ограничено [MAX_RETAINED_THREADS],
 * тред, закрытый на экране «Треды», освобождается сразу. Наблюдатель треда активен только пока
 * экран показан, поэтому удержанная сессия не ходит в сеть в фоне.
 */
internal class ThreadSessionStores : ViewModel() {
    private val stores = LinkedHashMap<ThreadAddress, ViewModelStore>()

    /** Хранилище сессии треда; самое давнее сверх лимита освобождается. */
    fun owner(address: ThreadAddress): ViewModelStoreOwner {
        val store = stores.remove(address) ?: ViewModelStore()
        stores[address] = store
        while (stores.size > MAX_RETAINED_THREADS) {
            val oldest = stores.keys.first()
            stores.remove(oldest)?.clear()
        }
        return RetainedThreadOwner(store)
    }

    /** Тред закрыт явно: его сессия больше не нужна. */
    fun drop(addresses: Collection<ThreadAddress>) {
        addresses.forEach { stores.remove(it)?.clear() }
    }

    val size: Int get() = stores.size

    override fun onCleared() {
        stores.values.forEach(ViewModelStore::clear)
        stores.clear()
    }
}

private class RetainedThreadOwner(
    override val viewModelStore: ViewModelStore,
) : ViewModelStoreOwner

internal val LocalThreadSessionStores = staticCompositionLocalOf<ThreadSessionStores?> { null }

/** Хранилище уровня Activity; вызывается вне NavHost, где владелец ViewModel — сама Activity. */
@Composable
internal fun rememberThreadSessionStores(workspace: WorkspaceController?): ThreadSessionStores {
    val stores: ThreadSessionStores = viewModel()
    LaunchedEffect(stores, workspace) {
        var previous: Set<ThreadAddress>? = null
        workspace
            ?.state
            ?.map { state -> state.tabs.map { it.address }.toSet() }
            ?.distinctUntilChanged()
            ?.collect { open ->
                previous?.let { stores.drop(it - open) }
                previous = open
            }
    }
    return stores
}

/** Владелец ViewModel сессии треда: удерживаемый, если хранилище есть, иначе запись навигации. */
@Composable
internal fun rememberThreadSessionOwner(
    address: ThreadAddress,
    fallback: ViewModelStoreOwner,
): ViewModelStoreOwner {
    val stores = LocalThreadSessionStores.current ?: return fallback
    return remember(stores, address) { stores.owner(address) }
}

internal const val MAX_RETAINED_THREADS = 4
