package com.solthariel.pawchan

import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadContentFlags
import com.solthariel.pawchan.thread.ThreadContentPresentation
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadPostChanges
import com.solthariel.pawchan.thread.ThreadPostDisplay
import com.solthariel.pawchan.thread.ThreadPostMetadataUi
import com.solthariel.pawchan.thread.ThreadPostUi
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.withPostNavigation

internal fun toolsPost(
    id: Long,
    text: String = "Текст",
    changes: ThreadPostChanges = ThreadPostChanges(),
    author: String = "Имя",
): ThreadPostUi =
    ThreadPostUi(
        id,
        PostDocument.plain(text),
        RichPostTextUi.create(PostDocument.plain(text)),
        ThreadPostMetadataUi(
            author,
            "05.10 09:01:11",
            false,
            ThreadPostDisplay(index = id.toInt(), header = AnnotatedString("#$id $author 05.10 09:01:11")),
            changes,
        ),
        emptyList(),
    )

internal fun toolsContent(posts: List<ThreadPostUi>): ThreadUiState.Content =
    ThreadUiState
        .Content(
            1,
            "Тема",
            posts,
            ThreadContentFlags(false),
            ThreadContentPresentation(false),
            ThreadContentStatistics(),
        ).withPostNavigation()
