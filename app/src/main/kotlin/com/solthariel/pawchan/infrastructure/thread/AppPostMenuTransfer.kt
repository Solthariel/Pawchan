package com.solthariel.pawchan.infrastructure.thread

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.thread.PostLinkResolver
import com.solthariel.pawchan.thread.PostMenuTransfer

internal class AppPostMenuTransfer(
    private val context: Context,
    private val links: PostLinkResolver? = null,
) : PostMenuTransfer {
    override fun postLink(
        address: ThreadAddress,
        postId: Long,
    ): String? = links?.postUrl(address, postId)

    override fun copy(text: String) {
        context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText("Пост", text))
    }

    fun share(text: String) {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(intent, "Поделиться ссылкой"))
    }
}
