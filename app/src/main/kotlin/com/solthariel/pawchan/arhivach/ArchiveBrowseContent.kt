package com.solthariel.pawchan.arhivach

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.FastScrollMode
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.core.ui.PostCardSurface
import com.solthariel.pawchan.core.ui.PostHeaderAuthor
import com.solthariel.pawchan.core.ui.PostHeaderAuthorUi
import com.solthariel.pawchan.core.ui.PostHeaderIndex
import com.solthariel.pawchan.core.ui.PostHeaderLayout
import com.solthariel.pawchan.core.ui.PostHeaderText
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.core.ui.PostStateMarker
import com.solthariel.pawchan.core.ui.PostStateMarkers
import com.solthariel.pawchan.core.ui.formatPostTime
import com.solthariel.pawchan.core.ui.isDefaultAnonymousName
import com.solthariel.pawchan.core.ui.postBodyTextStyle
import com.solthariel.pawchan.core.ui.postCollapseMaxLines
import com.solthariel.pawchan.core.ui.postContainerShape
import com.solthariel.pawchan.core.ui.postTextIsLimited
import com.solthariel.pawchan.core.ui.russianPlural
import com.solthariel.pawchan.core.ui.threadPostListPadding
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.infrastructure.media.MediaContextMenuHost
import com.solthariel.pawchan.infrastructure.thread.AppPostMenuTransfer
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaContentHost
import com.solthariel.pawchan.media.MediaMenuPlace
import com.solthariel.pawchan.media.MediaMenuRequest
import com.solthariel.pawchan.media.MediaViewerSelection
import com.solthariel.pawchan.media.PostAttachments
import com.solthariel.pawchan.media.rememberMediaSaveActions
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading
import com.solthariel.pawchan.thread.PostFooterActions
import com.solthariel.pawchan.thread.PostSelectionHost
import com.solthariel.pawchan.thread.PostTextContent
import com.solthariel.pawchan.thread.ReadOnlyPostMenu
import com.solthariel.pawchan.thread.SharedPostText
import com.solthariel.pawchan.thread.SharedPostTextPresentation

@Composable
internal fun ArchivePostList(
    state: ArchiveBrowseState,
    loaders: MediaLoaders,
    words: String,
    galleryRequest: Int,
    modifier: Modifier,
) {
    val data = rememberArchivePostListData(state.thread, words)
    val media = rememberArchiveMediaState(state.selectedThread, galleryRequest, data.presentation?.gallery)
    val hideStickers = LocalPostDisplayPreferences.current.hideStickers
    var menuPost by remember(state.selectedThread) { mutableStateOf<ArchivePostCardUi?>(null) }
    val previews = remember(state.selectedThread) { ArchivePreviewState() }
    val context = LocalContext.current
    val transfer = remember(context) { AppPostMenuTransfer(context) }
    val saves = rememberMediaSaveActions(loaders.mediaDownloadQueue)
    Box(modifier) {
        MediaContentHost(
            selection =
                MediaViewerSelection(media.opened, media.viewerItems(data.presentation, hideStickers)) {
                    media.opened = it
                },
            mediaLoaders = loaders.viewer,
            mediaSave = saves,
            onMenu = { media.menu = it },
            onClose = media::closeViewer,
        ) {
            val actions =
                ArchivePostActions(
                    media =
                        ArchiveMediaActions(
                            open = media::openPost,
                            download = saves.request,
                            menu = { media.menu = MediaMenuRequest(it, MediaMenuPlace.THUMBNAIL) },
                        ),
                    reference = { previews.open(it.post.value, replies = false) },
                    replies = { previews.open(it, replies = true) },
                    menu = { menuPost = it },
                )
            PostSelectionHost(Modifier.fillMaxSize()) {
                ArchivePostRows(state, data, loaders, words, actions)
            }
            data.presentation?.let { presentation ->
                ArchivePostPreviews(previews, presentation, loaders, actions, state.openThread.status)
            }
            ArchiveMediaGallery(data.presentation?.gallery, loaders, saves, media)
        }
    }
    MediaContextMenuHost(media.menu, loaders, saves, onNotice = {}, onClose = { media.menu = null })
    menuPost?.let { post ->
        val number = post.post.number?.toLongOrNull()
        val link =
            number
                ?.takeIf { data.presentation?.byNumber?.containsKey(it) == true }
                ?.let { archivePostLink(state.selectedThread, it) }
        ReadOnlyPostMenu(post.document, transfer, link) { menuPost = null }
    }
}

internal data class ArchiveMediaActions(
    val open: (MediaAttachmentUi) -> Unit,
    val download: (MediaAttachmentUi) -> Unit,
    val menu: (MediaAttachmentUi) -> Unit,
)

internal data class ArchivePostActions(
    val media: ArchiveMediaActions,
    val reference: (PostLink.Reference) -> Unit,
    val replies: (Long) -> Unit,
    val menu: (ArchivePostCardUi) -> Unit,
)

@Composable
private fun ArchivePostRows(
    state: ArchiveBrowseState,
    data: ArchivePostListData,
    loaders: MediaLoaders,
    words: String,
    actions: ArchivePostActions,
) {
    val visiblePosts = data.posts
    val listState = rememberLazyListState()
    val thumbnailLoading = rememberThumbnailLoading(loaders.thumbnailLoader, listState)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = threadPostListPadding(),
        ) {
            state.thread?.let {
                itemsIndexed(
                    visiblePosts,
                    key = { _, card -> "post:${state.selectedThread}:${card.ordinal}:${card.post.number.orEmpty()}" },
                    contentType = { _, _ -> "archive-post" },
                ) { index, card ->
                    Column {
                        ArchivePostRow(card, thumbnailLoading, actions, state.openThread.status, data.presentation)
                        PostSeparator(index, visiblePosts.lastIndex)
                    }
                }
                if (words.isNotBlank() && visiblePosts.isEmpty()) {
                    item { Text("Посты не найдены", Modifier.padding(16.dp)) }
                }
            }
        }
        PawchanFastScrollbar(
            state = listState,
            inputGesture = thumbnailLoading.gesture,
            mode = FastScrollMode.ITEMS,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
internal fun ArchivePostRow(
    card: ArchivePostCardUi,
    thumbnailLoading: ThumbnailLoading,
    actions: ArchivePostActions,
    threadStatus: String?,
    presentation: ArchiveThreadPresentationUi?,
) {
    val shape = postContainerShape()
    val number = card.post.number?.toLongOrNull()
    val replies = number?.let { presentation?.replies?.get(it) }.orEmpty()
    PostCardSurface(
        contentModifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(onClick = {}, onLongClick = { actions.menu(card) })
                .padding(8.dp),
    ) {
        ArchivePostHeader(card.ordinal, card.post, threadStatus.takeIf { card.ordinal == 1 })
        if (card.media.attachments.isNotEmpty()) {
            PostAttachments(
                card.media,
                thumbnailLoading,
                actions.media.open,
                actions.media.download,
                actions.media.menu,
            )
        }
        ArchivePostBody(
            card,
            replyCount = replies.size,
            onReplies = { number?.let(actions.replies) },
            onReference = actions.reference,
            onMenu = { actions.menu(card) },
        )
    }
}

@Composable
internal fun ArchivePostBody(
    card: ArchivePostCardUi,
    replyCount: Int = 0,
    onReplies: () -> Unit = {},
    onReference: (PostLink.Reference) -> Unit = {},
    onMenu: () -> Unit = {},
) {
    val preferences = LocalPostDisplayPreferences.current
    var expanded by rememberSaveable(card.ordinal, card.post.number) { mutableStateOf(false) }
    var limited by rememberSaveable(card.ordinal, card.post.number, preferences.collapseLines) { mutableStateOf(false) }
    val maxLines = postCollapseMaxLines(card.ordinal == 1, expanded, preferences.collapseLines)
    if (card.subject != null || card.document.plainText.isNotBlank()) {
        PostTextContent(card.subject, null, preferences.textSize) {
            if (card.document.plainText.isNotBlank()) {
                SharedPostText(
                    card.richText,
                    SharedPostTextPresentation(
                        style = postBodyTextStyle(preferences.textSize),
                        maxLines = maxLines,
                        onTextLayout = { result ->
                            limited = maxLines != Int.MAX_VALUE && postTextIsLimited(result, expanded, maxLines)
                        },
                        onLongPress = onMenu,
                    ),
                    onReference = onReference,
                )
            }
        }
    }
    PostFooterActions(
        replyLabel = if (replyCount > 0) russianPlural(replyCount, "ответ", "ответа", "ответов") else null,
        limited = limited && maxLines != Int.MAX_VALUE,
        enabled = true,
        onReplies = onReplies,
        onExpand = {
            expanded = true
            limited = false
        },
    )
}

@Composable
internal fun ArchivePostHeader(
    ordinal: Int,
    post: ArchivePost,
    threadStatus: String? = null,
) {
    val status = threadStatus.toArchiveStatus()
    val preferences = LocalPostDisplayPreferences.current
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val date =
        remember(post.details.timestamp, post.date, preferences) {
            post.details.timestamp?.let { formatPostTime(it, preferences) } ?: post.date
        }
    val markers =
        remember(post.details, status) {
            listOfNotNull(
                PostStateMarker.ORIGINAL_POSTER.takeIf { post.details.originalPoster },
                PostStateMarker.SAGE.takeIf { post.details.sage },
                status?.postMarker,
            )
        }
    PostHeaderLayout(
        leading = {
            PostHeaderIndex(deleted = false, index = ordinal)
            post.number?.let { PostHeaderText("#$it", secondary) }
            date?.let { PostHeaderText(it, secondary) }
            if (preferences.showStateMarkers) {
                val labels = status?.let { mapOf(it.postMarker to it.listMarker.description) }.orEmpty()
                PostStateMarkers(markers, labelOverrides = labels)
            }
        },
        byline = {
            val name = post.author?.takeUnless { isDefaultAnonymousName(it) && !preferences.showDefaultName }
            PostHeaderAuthor(PostHeaderAuthorUi(name = name))
        },
    )
}
