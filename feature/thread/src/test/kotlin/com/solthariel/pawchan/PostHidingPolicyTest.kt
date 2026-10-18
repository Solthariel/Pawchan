package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.postpreferences.PostPreferences
import com.solthariel.pawchan.thread.PostPreferenceAction
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadPostUi
import com.solthariel.pawchan.thread.changed
import com.solthariel.pawchan.thread.hiddenPostIds
import com.solthariel.pawchan.thread.normalizedPostName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostHidingPolicyTest {
    @Test
    fun replyTreeIsTransitiveHandlesCyclesAndDoesNotHideUnrelatedPosts() {
        val posts = listOf(referencePost(1, 3), referencePost(2, 1), referencePost(3, 2), toolsPost(4))
        assertEquals(setOf(1L, 2L, 3L), hiddenPostIds(posts, PostPreferences(replyTrees = setOf(1))))
        assertEquals(setOf(1L, 3L), hiddenPostIds(posts, PostPreferences(replyTrees = setOf(1), shown = setOf(2))))
    }

    @Test
    fun nameRulesNormalizeAnonymousAndShowingOnePostIsAnException() {
        val posts = listOf(toolsPost(1, author = "Имя"), toolsPost(2, author = " имя "), toolsPost(3, author = ""))
        val rules = PostPreferences(names = setOf("имя"), shown = setOf(1))
        assertEquals(setOf(2L), hiddenPostIds(posts, rules))
        assertEquals("аноним", normalizedPostName(null))
    }

    @Test
    fun similarPostsUseEightyPercentWordOverlapAndIgnoreEmptyTexts() {
        val posts =
            listOf(
                toolsPost(1, "один два три четыре пять"),
                toolsPost(2, "один два три четыре"),
                toolsPost(3, "совсем другое"),
            )
        assertEquals(setOf(1L, 2L), hiddenPostIds(posts, PostPreferences(similar = setOf("один два три четыре пять"))))
        assertTrue(hiddenPostIds(posts, PostPreferences(similar = setOf(""))).isEmpty())
    }

    @Test
    fun showingAllRetainsOwnMarksButRemovesEveryHideRule() {
        val rules = PostPreferences(own = setOf(3), hidden = setOf(1), names = setOf("имя"), shown = setOf(2))
        assertEquals(PostPreferences(own = setOf(3)), rules.changed(PostPreferenceAction.SHOW_ALL, toolsPost(1)))
    }

    private fun referencePost(
        id: Long,
        parent: Long,
    ): ThreadPostUi {
        val document =
            PostDocument(
                listOf(PostTextRun(">>$parent", link = PostLink.Reference(requireNotNull(PostId.parse(parent))))),
            )
        return toolsPost(id).copy(document = document, richText = RichPostTextUi.create(document))
    }
}
