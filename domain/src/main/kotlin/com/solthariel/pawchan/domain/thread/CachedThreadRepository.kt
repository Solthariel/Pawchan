package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot

interface CachedThreadRepository : ThreadRepository {
    suspend fun cachedThread(address: ThreadAddress): ThreadSnapshot?

    /**
     * Копию треда можно показать при повторном открытии без запроса в сеть:
     * тред уже читался в этой сессии. Новые посты всё равно подгрузит наблюдатель или «Обновить».
     */
    suspend fun isCachedThreadCurrent(address: ThreadAddress): Boolean = false
}
