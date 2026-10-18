package com.solthariel.pawchan.media.video

import android.os.Handler
import android.os.Looper

/**
 * Запас сессий просмотрщика: следующее видео открывается заранее (на паузе, без поверхности),
 * а видео, с которого ушли свайпом, живёт ещё [LEFT_EXPIRY_MS] — при возврате открывается без загрузки, с начала.
 * Держит не больше [CAPACITY] сессий: аппаратных декодеров на устройстве немного.
 */
internal object VideoSessionPool {
    private class Entry(
        val session: VideoPlaybackSession,
        val playing: Boolean,
        val expiry: Runnable?,
    )

    private val main = Handler(Looper.getMainLooper())
    private val entries = LinkedHashMap<String, Entry>()

    /** Сессия для этого файла и нужно ли ей играть дальше; null — открывать заново. */
    fun take(url: String): Pair<VideoPlaybackSession, Boolean>? {
        val entry = entries.remove(url)
        entry?.expiry?.let(main::removeCallbacks)
        val usable = entry?.takeUnless { it.session.status.value.stage == VideoStage.ERROR }
        if (usable == null) entry?.session?.close()
        // Новая страница ещё без кадра: превью видно с первого же кадра композиции, а не после эффекта.
        usable?.session?.mutableVideoOutput?.value = false
        return usable?.let { it.session to it.playing }
    }

    /** Ушли с видео свайпом: пауза, а не закрытие; запоминается, играло ли оно. */
    fun retain(session: VideoPlaybackSession) {
        val playing = session.status.value.playing
        session.pause()
        val expiry = Runnable { drop(session.url, session) }
        put(session.url, Entry(session, playing, expiry))
        main.postDelayed(expiry, LEFT_EXPIRY_MS)
    }

    /** Следующее видео открывается заранее; при переходе на него запускается сразу. */
    fun preload(
        url: String,
        create: () -> VideoPlaybackSession,
    ) {
        if (url in entries) return
        val session = create()
        session.startAt(0L, playing = false)
        put(url, Entry(session, playing = true, expiry = null))
    }

    /** Просмотрщик закрыт: запас не нужен. */
    fun clear() {
        entries.values.forEach { entry ->
            entry.expiry?.let(main::removeCallbacks)
            entry.session.close()
        }
        entries.clear()
    }

    private fun put(
        url: String,
        entry: Entry,
    ) {
        entries.remove(url)?.let { old ->
            old.expiry?.let(main::removeCallbacks)
            if (old.session !== entry.session) old.session.close()
        }
        entries[url] = entry
        while (entries.size > CAPACITY) {
            val oldest = entries.keys.first()
            entries.remove(oldest)?.let { old ->
                old.expiry?.let(main::removeCallbacks)
                old.session.close()
            }
        }
    }

    private fun drop(
        url: String,
        session: VideoPlaybackSession,
    ) {
        val entry = entries[url]?.takeIf { it.session === session } ?: return
        entries.remove(url)
        entry.session.close()
    }
}

private const val LEFT_EXPIRY_MS = 5_000L
private const val CAPACITY = 2
