package com.solthariel.pawchan.readme

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Picks the thread to film from the board's public JSON: lively, quick to open, with a video near the top and no
 * crude words in the posts the screens show, because these pictures go on the public GitHub page.
 */
internal class ReadmeThreadPicker(
    private val host: String,
    private val board: String,
) {
    fun pick(): ReadmePicks {
        val seen = mutableListOf<ReadmeFilm>()
        for (thread in threads().take(SEARCH_THREADS)) {
            seen += runCatching { film(thread) }.getOrNull() ?: continue
            if (seen.count { it.clean && it.video != null } == VIDEO_SEARCH_THREADS) break
        }
        check(seen.isNotEmpty()) { "no thread of /$board/ could be read" }
        val clean = seen.filter { it.clean }
        // Film the clean thread whose first video comes earliest, so the viewer reaches it in a few swipes.
        val best =
            clean
                .mapNotNull { film -> film.video?.let { film to it.index } }
                .minByOrNull { (_, index) -> index }
                ?.first ?: clean.firstOrNull() ?: seen.first()
        return ReadmePicks(best.thread, best.video, crudeSkipped = seen.size - clean.size, clean = best.clean)
    }

    private fun threads(): List<Long> {
        val catalog = JSONObject(get("https://$host/$board/catalog.json")).getJSONArray("threads")
        val threads =
            (0 until catalog.length())
                .map(catalog::getJSONObject)
                .filter { it.optInt("sticky") == 0 && it.optInt("closed") == 0 }
                // A lively thread that still opens fast: endless generals with thousands of posts load too long.
                .sortedWith(
                    compareBy<JSONObject> { it.optInt("posts_count") !in COMFORTABLE_POSTS }
                        .thenByDescending { it.optInt("files_count", it.optInt("posts_count")) },
                ).map { it.getLong("num") }
        check(threads.isNotEmpty()) { "no open threads on /$board/" }
        return threads
    }

    private fun film(thread: Long): ReadmeFilm {
        val json =
            JSONObject(get("https://$host/$board/res/$thread.json"))
                .getJSONArray("threads")
                .getJSONObject(0)
                .getJSONArray("posts")
        val posts = (0 until json.length()).map(json::getJSONObject)
        return ReadmeFilm(thread, firstVideo(posts), posts.take(SHOWN_POSTS).none(::crude))
    }

    /** The first video of a thread and how many files come before it, in the order the viewer pages them. */
    private fun firstVideo(posts: List<JSONObject>): ReadmeVideo? {
        var index = 0
        for (post in posts) {
            val files = post.optJSONArray("files") ?: continue
            for (file in 0 until files.length()) {
                val path = files.getJSONObject(file).optString("path")
                if (path.endsWith(".mp4") || path.endsWith(".webm")) return ReadmeVideo("https://$host$path", index)
                index++
            }
        }
        return null
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "Pawchan readme shots")
        return try {
            connection.inputStream.use { it.readBytes().decodeToString() }
        } finally {
            connection.disconnect()
        }
    }
}

/** Everything a post puts on screen as text: subject, message without markup, file names in the viewer title. */
private fun crude(post: JSONObject): Boolean {
    val files = post.optJSONArray("files") ?: JSONArray()
    val names =
        (0 until files.length())
            .map(files::getJSONObject)
            .flatMap { listOf(it.optString("fullname"), it.optString("displayname")) }
    val text = listOf(post.optString("subject"), post.optString("comment").replace(MARKUP, " ")) + names
    return CRUDE.containsMatchIn(text.joinToString(" ").lowercase().replace('ё', 'е'))
}

internal data class ReadmePicks(
    val thread: Long,
    val video: ReadmeVideo?,
    val crudeSkipped: Int = 0,
    val clean: Boolean = true,
)

internal data class ReadmeVideo(
    val url: String,
    val index: Int,
)

private data class ReadmeFilm(
    val thread: Long,
    val video: ReadmeVideo?,
    val clean: Boolean,
)

private const val VIDEO_SEARCH_THREADS = 6
private const val SEARCH_THREADS = 40
private const val SHOWN_POSTS = 12
private const val TIMEOUT_MS = 20_000

private val COMFORTABLE_POSTS = 40..600
private val MARKUP = Regex("<[^>]*>|&[a-z#0-9]+;")

/** Mat, sexual words and slurs, Russian and English; a false alarm only skips one more thread. */
private val CRUDE =
    Regex(
        listOf(
            "(?<!\\p{L})(за|у|вы|на|до|от|отъ|про|съ|по|под|подъ|раз|разъ|пере|при|недо|долбо|въ|из|изъ)?еб",
            "(?<!стра)ху[йеяию]",
            "пизд|(?<!\\p{L})бля|шлюх|шалав|проститут|пид[оа]р|пидр|педик|залуп|дроч",
            "мудак|мудил|гандон|говн|дерьм|жоп|сиськ|сисек|письк|писек|писюн",
            "(?<!\\p{L})член|секс|порн|хентай|(?<!\\p{L})трах|изнасил",
            "сучк|(?<!\\p{L})сук[аиеу]|нигг|нигер|чурк|хохл|быдл",
            "(?<!\\p{L})(fuck|shit|bitch|cunt|whore|slut|porn|hentai|dick|cock|pussy|fag|nigg|sex|rape)",
        ).joinToString("|"),
    )
