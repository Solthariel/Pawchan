package com.solthariel.pawchan.thread

/** Occurrences, not post IDs: the same post can appear twice in a reference chain. */
internal data class PreviewStackEntry(
    val id: String,
    val target: PostPreviewTarget,
)

internal class PreviewStackPath {
    private var previous = emptyList<PreviewStackEntry>()

    fun reconcile(targets: List<PostPreviewTarget>): List<PreviewStackEntry> {
        val common = previous.zip(targets).takeWhile { (entry, target) -> entry.target == target }.size
        var prefix =
            previous
                .take(common)
                .lastOrNull()
                ?.id
                .orEmpty()
        previous = previous.take(common) +
            targets.drop(common).map { target ->
                prefix = "$prefix>${target.address}:${target.postId}:${target.replies}"
                PreviewStackEntry(prefix, target)
            }
        return previous
    }
}

internal const val RETAINED_PREVIEW_WINDOWS = 5
