package com.solthariel.pawchan.infrastructure.thread

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.R
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.infrastructure.sites.LocalForumMirror
import com.solthariel.pawchan.infrastructure.sites.appForumWebLinks
import com.solthariel.pawchan.thread.PostLinkBindings
import com.solthariel.pawchan.thread.ThreadPostLinkHost
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.WorkspaceThreadRepository
import com.solthariel.pawchan.workspace.openThread
import kotlinx.coroutines.launch

@Composable
internal fun AppPostLinkHost(
    onOpenBoard: (BoardAddress) -> Unit,
    content: @Composable () -> Unit,
) {
    val mirror = LocalForumMirror.current
    val workspace = LocalWorkspace.current
    val resolver = remember(mirror) { AppPostLinkResolver(appForumWebLinks(mirror)) }
    val context = LocalContext.current
    val transfer = remember(context, resolver) { AppPostMenuTransfer(context, resolver) }
    val openThread: ((ThreadAddress, Long?) -> Unit)? =
        workspace?.let { binding ->
            { address, post -> binding.openThread(address, post) }
        }
    val scope = rememberCoroutineScope()
    val openFilePost: ((ThreadAddress, String) -> Unit)? =
        workspace?.let { binding ->
            val repository = binding.loaders.threadRepository?.let { WorkspaceThreadRepository(it, binding.controller) }
            val open: (ThreadAddress, String) -> Unit = { address, url ->
                scope.launch {
                    val lookup =
                        repository?.findFilePost(address, url) { resolver.filePath(address, it) }
                            ?: FilePostLookup.Unavailable
                    if (lookup == FilePostLookup.Missing) {
                        Toast.makeText(context, R.string.link_file_post_missing, Toast.LENGTH_SHORT).show()
                    }
                    binding.openThread(address, (lookup as? FilePostLookup.Found)?.post)
                }
            }
            open
        }
    val bindings =
        PostLinkBindings(
            resolver,
            transfer,
            workspace?.loaders?.mediaDownloadQueue,
            openThread,
            onOpenBoard,
            openFilePost,
        )
    ThreadPostLinkHost(bindings, content)
}
