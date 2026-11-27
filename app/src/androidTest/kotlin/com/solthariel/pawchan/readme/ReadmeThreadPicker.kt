package com.solthariel.pawchan.readme

import org.json.JSONObject
import java.io.InputStream
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
        return ReadmePicks(best.thread, best.video, seen.size - clean.size, best.clean, best.pictures)
    }

    /** The thread the owner chose: its first video and pictures for the composer, still checked for crude words. */
    fun thread(number: Long): ReadmePicks {
        val film = film(number)
        return ReadmePicks(number, film.video, clean = film.clean, pictures = film.pictures)
    }

    fun bytes(url: String): ByteArray = connect(url) { it.readBytes() }

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
        return ReadmeFilm(thread, firstVideo(posts), posts.take(SHOWN_POSTS).none(::crude), pictures(posts))
    }

    /** Light still pictures from the replies, in thread order, to attach in the composer. */
    private fun pictures(posts: List<JSONObject>): List<ReadmePicture> =
        posts
            .drop(1)
            .flatMap(::files)
            .filter { it.optInt("type") in STILL_TYPES && it.optInt("size") in PICTURE_KB }
            .take(PICTURES)
            .mapIndexed { index, file ->
                val path = file.optString("path")
                ReadmePicture("https://$host$path", "pawchan-${index + 1}.${path.substringAfterLast('.')}")
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

    private fun get(url: String): String = connect(url) { it.readBytes().decodeToString() }

    private fun <T> connect(
        url: String,
        read: (InputStream) -> T,
    ): T {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "Pawchan readme shots")
        return try {
            connection.inputStream.use(read)
        } finally {
            connection.disconnect()
        }
    }
}

/** Everything a post puts on screen as text: subject, message without markup, file names in the viewer title. */
private fun crude(post: JSONObject): Boolean {
    val names = files(post).flatMap { listOf(it.optString("fullname"), it.optString("displayname")) }
    val text = listOf(post.optString("subject"), post.optString("comment").replace(MARKUP, " ")) + names
    return CRUDE.containsMatchIn(text.joinToString(" ").lowercase().replace('ё', 'е'))
}

/** Files the owner chose for the composer, in the given order: pawchan-1.mp4, pawchan-2.jpg and so on. */
internal fun readmeAttachments(urls: List<String>): List<ReadmePicture> =
    urls.mapIndexed { index, url ->
        ReadmePicture(url, "pawchan-${index + 1}.${url.substringAfterLast('.').substringBefore('?').lowercase()}")
    }

private fun files(post: JSONObject): List<JSONObject> {
    val files = post.optJSONArray("files") ?: return emptyList()
    return (0 until files.length()).map(files::getJSONObject)
}

internal data class ReadmePicks(
    val thread: Long,
    val video: ReadmeVideo?,
    val crudeSkipped: Int = 0,
    val clean: Boolean = true,
    val pictures: List<ReadmePicture> = emptyList(),
)

/** A file of the thread and the name it gets in Download/[PICTURES_FOLDER] for the composer. */
internal data class ReadmePicture(
    val url: String,
    val name: String,
)

internal data class ReadmeVideo(
    val url: String,
    val index: Int,
)

private data class ReadmeFilm(
    val thread: Long,
    val video: ReadmeVideo?,
    val clean: Boolean,
    val pictures: List<ReadmePicture>,
)

private const val VIDEO_SEARCH_THREADS = 6
private const val SEARCH_THREADS = 40
private const val SHOWN_POSTS = 12
private const val TIMEOUT_MS = 20_000
private const val PICTURES = 3
private val STILL_TYPES = setOf(1, 2)
private val PICTURE_KB = 100..1_500

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
