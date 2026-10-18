package com.solthariel.pawchan.infrastructure.thread

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.thread.ThreadPostStorage
import com.solthariel.pawchan.workspace.WorkspaceController
import com.solthariel.pawchan.workspace.setOwn

internal class AppThreadPostStorage(
    private val workspace: WorkspaceController,
) : ThreadPostStorage {
    override fun own(
        address: PostAddress,
        own: Boolean,
    ) {
        workspace.setOwn(address.thread, address.post.value, own)
    }
}
