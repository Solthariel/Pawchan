package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.postpreferences.PostPreferences

internal enum class PostPreferenceAction { OWN, HIDE, HIDE_TREE, HIDE_NAME, HIDE_SIMILAR, SHOW, SHOW_ALL }

internal fun PostPreferences.changed(
    action: PostPreferenceAction,
    post: ThreadPostUi,
): PostPreferences =
    when (action) {
        PostPreferenceAction.OWN -> copy(own = if (post.id in own) own - post.id else own + post.id)
        PostPreferenceAction.HIDE -> copy(hidden = hidden + post.id, shown = shown - post.id)
        PostPreferenceAction.HIDE_TREE -> copy(replyTrees = replyTrees + post.id, shown = emptySet())
        PostPreferenceAction.HIDE_NAME -> copy(names = names + normalizedPostName(post.author), shown = emptySet())
        PostPreferenceAction.HIDE_SIMILAR -> copy(similar = similar + post.document.plainText, shown = emptySet())
        PostPreferenceAction.SHOW -> copy(hidden = hidden - post.id, shown = shown + post.id)
        PostPreferenceAction.SHOW_ALL -> PostPreferences(own = own)
    }
