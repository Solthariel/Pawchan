package com.solthariel.pawchan.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.solthariel.pawchan.infrastructure.thread.rememberAppPostPreviewResources
import com.solthariel.pawchan.thread.SavedPostActions
import com.solthariel.pawchan.thread.SavedPostResources
import com.solthariel.pawchan.thread.SavedPostScreen
import com.solthariel.pawchan.thread.ThreadMediaResources

@Composable
internal fun WorkspaceSavedPost(
    item: WorkspacePostUi,
    showThreadLabel: Boolean = false,
) {
    val binding = LocalWorkspace.current ?: return
    val preview = rememberAppPostPreviewResources() ?: return
    val loaders = binding.loaders
    val actions = SavedPostActions { binding.controller.readReplies(item) }
    Column {
        if (showThreadLabel) androidx.compose.material3.Text(item.label)
        SavedPostScreen(
            item.presentation,
            SavedPostResources(
                ThreadMediaResources(
                    loaders.thumbnailLoader,
                    loaders.viewer,
                    loaders.mediaDownloadQueue,
                    vocaroo = loaders.vocaroo,
                ),
                preview,
            ),
            actions,
        )
    }
}
