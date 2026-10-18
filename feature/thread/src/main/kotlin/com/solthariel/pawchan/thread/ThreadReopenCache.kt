package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.CachedThreadRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository

/** Решает, можно ли при повторном открытии треда обойтись копией из памяти без запроса в сеть. */
internal class ThreadReopenCache(
    private val address: ThreadAddress,
    private val repository: ThreadRepository,
    private val watches: ThreadWatchRepository,
) {
    /** Копия актуальна, пока отметка наблюдателя не новее последнего поста в ней. */
    suspend fun isCurrent(cached: ThreadSnapshot): Boolean {
        val remembered = (repository as? CachedThreadRepository)?.isCachedThreadCurrent(address) == true
        val subscription = if (remembered) watches.load(address) else null
        val latest = cached.posts.maxOfOrNull { it.address.post.value }
        return when {
            subscription !is DomainOutcome.Success || latest == null -> false
            else -> (subscription.value?.lastKnownPostId?.value ?: latest) <= latest
        }
    }
}
