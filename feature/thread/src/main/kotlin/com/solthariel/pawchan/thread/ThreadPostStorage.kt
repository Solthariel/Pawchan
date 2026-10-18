package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.model.PostAddress

/** Menu-only state observation; persistence and its lifetime remain app-owned. */
interface ThreadPostStorage {
    fun own(
        address: PostAddress,
        own: Boolean,
    )
}
