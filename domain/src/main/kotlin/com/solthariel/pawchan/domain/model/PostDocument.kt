package com.solthariel.pawchan.domain.model

enum class PostTextStyle {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    GREENTEXT,
    RED,
    SPOILER,
}

sealed interface PostLink {
    data class Reference(
        val post: PostId,
        val thread: ThreadAddress? = null,
    ) : PostLink

    data class External(
        val url: RemoteUrl,
    ) : PostLink
}

data class PostTextRun(
    val text: String,
    val styles: Set<PostTextStyle> = emptySet(),
    val link: PostLink? = null,
)

class PostDocument(
    runs: List<PostTextRun>,
) {
    val runs: List<PostTextRun> = runs.filter { it.text.isNotEmpty() }.toList()
    val plainText: String = this.runs.joinToString(separator = "", transform = PostTextRun::text)

    companion object {
        fun plain(text: String): PostDocument =
            PostDocument(
                listOf(PostTextRun(text)),
            )
    }
}

data class PostMetadata(
    val name: String?,
    val date: String?,
    val sage: Boolean,
    val subject: String? = null,
    val nameColor: Int? = null,
    val badges: List<PostBadge> = emptyList(),
    val originalPoster: Boolean = false,
)

data class PostRating(
    val likes: Int,
    val dislikes: Int,
) {
    init {
        require(likes >= 0 && dislikes >= 0)
    }
}

data class PostContent(
    val document: PostDocument,
    val metadata: PostMetadata,
    /** Счётчики оценок; null — доска не поддерживает оценки. Не участвует в истории изменений поста. */
    val rating: PostRating? = null,
    val details: PostDetails = PostDetails(),
)

/** Сведения сайта о посте вне текста: точное время, трипкод, email и бан (как значки поста в Dashchan). */
data class PostDetails(
    /** Unix-время в секундах; null — сайт не прислал, тогда показывается строка даты сайта. */
    val timestamp: Long? = null,
    val tripcode: String? = null,
    /** Email автора, кроме «sage»: sage — отдельный флаг метаданных. */
    val email: String? = null,
    val banned: Boolean = false,
)

data class PostBadge(
    val label: String,
    val image: RemoteUrl,
    val symbol: String? = null,
)
