package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.ThreadAddress

interface ThreadDeletionStore {
    suspend fun contains(address: ThreadAddress): Boolean

    suspend fun markDeleted(address: ThreadAddress)
}
