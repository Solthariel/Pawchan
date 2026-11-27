package com.solthariel.pawchan.infrastructure.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.settings.hiddenPostPlaceholders
import com.solthariel.pawchan.thread.PostPreviewActions
import com.solthariel.pawchan.thread.PostPreviewResources
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.PendingPreviewReply
import com.solthariel.pawchan.workspace.WorkspaceBinding
import com.solthariel.pawchan.workspace.WorkspaceThreadRepository
import kotlinx.coroutines.flow.map

@Composable
internal fun rememberAppPostPreviewResources(): PostPreviewResources? {
    val binding = LocalWorkspace.current ?: return null
    val repository =
        remember(binding.loaders.threadRepository, binding.controller) {
            binding.loaders.threadRepository?.let { WorkspaceThreadRepository(it, binding.controller) }
        }
    val actions = remember(binding.controller, binding.navigate) { AppPostPreviewActions(binding) }
    return PostPreviewResources(
        repository,
        binding.loaders.thumbnailLoader,
        actions,
        binding.appSettings?.appSettings?.map { it.contentFilters } ?: kotlinx.coroutines.flow.flowOf(emptyList()),
        binding.appSettings.hiddenPostPlaceholders(),
    )
}

private class AppPostPreviewActions(
    private val binding: WorkspaceBinding,
) : PostPreviewActions {
    override fun canReply(address: ThreadAddress): Boolean =
        binding.sources
            .firstOrNull { it.site == address.site }
            ?.capabilities
            ?.replies == true

    override fun prepareReply(
        address: ThreadAddress,
        text: String,
    ) {
        if (canReply(address)) binding.controller.pendingReply.value = PendingPreviewReply(address, text)
    }

    override fun navigate(address: ThreadAddress) {
        binding.navigate(address)
    }
}
