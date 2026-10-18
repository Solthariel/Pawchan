package com.solthariel.pawchan.domain.model

@ConsistentCopyVisibility
data class ThreadStatistics private constructor(
    val replies: Int,
    val files: Int,
    val images: Int,
    val knownMediaBytes: Long,
    val mediaBytesComplete: Boolean,
) {
    companion object {
        fun create(
            replies: Int,
            files: Int,
            images: Int,
            knownMediaBytes: Long = 0L,
            mediaBytesComplete: Boolean = files == 0,
        ): ThreadStatistics? =
            ThreadStatistics(replies, files, images, knownMediaBytes, mediaBytesComplete)
                .takeIf {
                    replies >= 0 &&
                        files >= 0 &&
                        images in 0..files &&
                        knownMediaBytes >= 0L
                }
    }
}

data class ThreadFlags(
    val pinned: Boolean,
    val closed: Boolean,
    val archived: Boolean,
    val endless: Boolean = false,
)

data class BoardDescriptor(
    val address: BoardAddress,
    val title: String,
    val worksafe: Boolean,
    val subjectsEnabled: Boolean? = null,
)

class CatalogThread(
    val address: ThreadAddress,
    val subject: String?,
    val content: PostContent,
    attachments: List<MediaAttachment>,
    val statistics: ThreadStatistics,
    val flags: ThreadFlags,
    val createdAtEpochSeconds: Long? = null,
) {
    val attachments: List<MediaAttachment> = attachments.toList()
}

class CatalogSnapshot(
    val board: BoardDescriptor,
    threads: List<CatalogThread>,
    val boardSpeed: Int = 0,
) {
    val threads: List<CatalogThread> = threads.toList()
}

class BoardPost(
    val address: PostAddress,
    val content: PostContent,
    attachments: List<MediaAttachment>,
    /** Поста уже нет на сервере, но он сохранён локально (как в Dashchan). */
    val deleted: Boolean = false,
) {
    val attachments: List<MediaAttachment> = attachments.toList()

    fun withDeleted(value: Boolean): BoardPost {
        if (value == deleted) return this
        return BoardPost(address, content, attachments, value)
    }
}

class ThreadSnapshot(
    val address: ThreadAddress,
    val subject: String?,
    val flags: ThreadFlags,
    posts: List<BoardPost>,
    val uniquePosters: Int = 0,
    val subjectsEnabled: Boolean? = null,
    /** Тег треда (2ch: поле tags), без слэшей; показывается как /tag/. */
    val tag: String? = null,
) {
    val posts: List<BoardPost> = posts.toList()

    /** Посты, которых уже нет на сервере, но которые сохранены локально. */
    val deletedPosts: Set<PostId> by lazy {
        this.posts
            .filter { it.deleted }
            .map { it.address.post }
            .toSet()
    }

    /** Тот же тред с другим набором постов; удалёнными становятся ровно [deletedPosts]. */
    fun withPosts(
        posts: List<BoardPost>,
        deletedPosts: Set<PostId> = emptySet(),
    ): ThreadSnapshot =
        ThreadSnapshot(
            address,
            subject,
            flags,
            posts.map { it.withDeleted(it.address.post in deletedPosts) },
            uniquePosters,
            subjectsEnabled,
            tag,
        )

    fun withFlags(flags: ThreadFlags): ThreadSnapshot =
        ThreadSnapshot(
            address,
            subject,
            flags,
            posts,
            uniquePosters,
            subjectsEnabled,
            tag,
        )
}
