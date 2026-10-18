package com.solthariel.pawchan

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.thread.PostLinkBindings
import com.solthariel.pawchan.thread.PostLinkResolver
import com.solthariel.pawchan.thread.PostLinkTarget
import com.solthariel.pawchan.thread.PostMenuTransfer
import com.solthariel.pawchan.thread.ThreadPostLinkHost

/** Protocol-independent UI fixture. Real adapter classification is tested in app. */
@Composable
internal fun ThreadTestLinkHost(content: @Composable () -> Unit) {
    val resolver =
        object : PostLinkResolver {
            override fun postUrl(
                address: ThreadAddress,
                postId: Long,
            ): String = "https://2ch.hk/${address.board.board.value}/res/${address.thread.value}.html#$postId"

            override fun resolve(
                link: PostLink,
                context: ThreadAddress?,
            ): PostLinkTarget? =
                when (link) {
                    is PostLink.Reference -> {
                        (link.thread ?: context)?.let { PostLinkTarget(postUrl(it, link.post.value), it) }
                    }

                    is PostLink.External -> {
                        PostLinkTarget(link.url.value)
                    }
                }
        }
    val transfer =
        object : PostMenuTransfer {
            override fun copy(text: String) = Unit
        }
    ThreadPostLinkHost(PostLinkBindings(resolver, transfer), content)
}
