package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDetails
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.PostRating
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import kotlinx.serialization.json.Json

internal class ThreadPayloadDecoder(
    private val json: Json,
) {
    fun decode(
        address: ThreadAddress,
        payload: String,
    ): ThreadSnapshot? {
        val response = json.decodeFromString<ThreadResponseDto>(payload)
        val postElements =
            response
                .threads
                .singleOrNull()
                ?.posts
        val posts =
            postElements?.mapNotNull { element ->
                json.decodeElementOrNull<ThreadPostDto>(element)
            }
        val opener = posts?.firstOrNull { it.parent == 0L && it.num == address.thread.value }
        val mappedPosts =
            posts?.mapNotNull { it.toDomain(json, address, response.board?.likesEnabled == true) }?.map { post ->
                if (response.board?.subjectsEnabled == false) {
                    BoardPost(
                        post.address,
                        post.content.copy(metadata = post.content.metadata.copy(subject = null)),
                        post.attachments,
                    )
                } else {
                    post
                }
            }
        return if (opener != null && mappedPosts != null) {
            ThreadSnapshot(
                address = address,
                subject = opener.subject.takeIf(String::isNotBlank),
                flags =
                    ThreadFlags(
                        pinned = opener.sticky > 0,
                        closed = opener.closed > 0,
                        archived = false,
                        endless = opener.endless > 0,
                    ),
                posts = mappedPosts,
                uniquePosters = response.uniquePosters.coerceAtLeast(0),
                subjectsEnabled = response.board?.subjectsEnabled,
                tag =
                    opener.tags
                        ?.trim()
                        ?.trim('/')
                        ?.takeIf(String::isNotBlank),
            )
        } else {
            null
        }
    }
}

private fun ThreadPostDto.toDomain(
    json: Json,
    thread: ThreadAddress,
    likesEnabled: Boolean,
): BoardPost? {
    val postId = PostId.parse(num)
    val attachments = files.orEmpty().toDomainAttachments(json)
    val belongsToThread = parent == 0L || parent == thread.thread.value
    return if (postId != null && belongsToThread) {
        BoardPost(
            address = PostAddress(thread, postId),
            content =
                PostContent(
                    document = PostDocumentParser.parse(comment),
                    metadata = toPostMetadata(),
                    rating = toPostRating(likesEnabled),
                    details = toPostDetails(),
                ),
            attachments = attachments,
        )
    } else {
        null
    }
}

private fun ThreadPostDto.toPostMetadata(): PostMetadata {
    val identity = decodeDvachPostIdentity(name, email)
    return PostMetadata(
        name = identity.name,
        date = date?.takeIf(String::isNotBlank),
        sage = identity.sage,
        subject = decodePostAuthor(subject),
        nameColor = decodeAuthorColor(name),
        badges = decodePostBadges(icon),
        originalPoster = decodeOriginalPoster(op),
    )
}

private fun ThreadPostDto.toPostDetails(): PostDetails {
    val address = email?.trim()?.removePrefix("mailto:")?.takeIf(String::isNotBlank)
    return PostDetails(
        timestamp = timestamp.takeIf { it > 0L },
        tripcode = decodePostAuthor(trip),
        email = address?.takeUnless { it.equals("sage", ignoreCase = true) },
        banned = banned > 0,
    )
}

// Счётчики есть в JSON любой доски; показываем их только там, где 2ch включил оценки.
private fun ThreadPostDto.toPostRating(likesEnabled: Boolean): PostRating? =
    if (likesEnabled) PostRating((likes ?: 0).coerceAtLeast(0), (dislikes ?: 0).coerceAtLeast(0)) else null
