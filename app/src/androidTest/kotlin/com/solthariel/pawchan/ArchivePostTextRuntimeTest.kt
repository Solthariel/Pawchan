package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.solthariel.pawchan.arhivach.ArchivePostBody
import com.solthariel.pawchan.arhivach.toArchiveThreadPresentationUi
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.archive.ArchivePostDetails
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArchivePostTextRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun localReferenceUsesTheStandardPostTap() {
        val link = PostLink.Reference(requireNotNull(PostId.parse(100)))
        val document = PostDocument(listOf(PostTextRun(">>100", link = link)))
        val post = ArchivePost("101", null, null, document.plainText, emptyList(), details = ArchivePostDetails(document = document))
        val op = ArchivePost("100", null, null, "OP", emptyList())
        val card = ArchiveThread(55, "title", listOf(op, post)).toArchiveThreadPresentationUi().posts.last()
        var opened: Long? = null
        compose.setContent { MaterialTheme { ArchivePostBody(card, onReference = { opened = it.post.value }) } }

        compose.onNodeWithText(">>100").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(100L, opened) }
    }

    @Test
    fun holdingBodyOpensThePostMenu() {
        val post = ArchivePost("101", null, null, "post body", emptyList())
        val card = ArchiveThread(55, "title", listOf(post)).toArchiveThreadPresentationUi().posts.single()
        var opened = false
        compose.setContent { MaterialTheme { ArchivePostBody(card, onMenu = { opened = true }) } }

        compose.onNodeWithText("post body").performTouchInput { longClick(center) }
        compose.runOnIdle { assertEquals(true, opened) }
    }
}
