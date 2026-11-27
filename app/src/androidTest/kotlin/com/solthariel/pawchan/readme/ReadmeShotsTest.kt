package com.solthariel.pawchan.readme

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Screens for the GitHub page, filmed on a live board. It runs only from scripts/readme-shots.ps1, which installs a
 * release-speed copy under its own package, wipes it and passes readmeShots=1, so the regular device smoke skips it.
 */
class ReadmeShotsTest {
    @Test
    fun filmReadmeScreens() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Only for scripts/readme-shots.ps1", arguments.getString("readmeShots") == "1")
        val device = ReadmeDevice(arguments.getString("target") ?: InstrumentationRegistry.getInstrumentation().targetContext.packageName)
        device.prepare()
        try {
            ReadmeFlow(device, arguments.getString("board") ?: DEFAULT_BOARD).run()
        } finally {
            device.finish()
        }
    }
}

/** Every step is independent: a failed one leaves a fail-*.png and a line in the log, the rest still run. */
internal class ReadmeFlow(
    private val device: ReadmeDevice,
    private val board: String,
) {
    fun run() {
        val picks = device.step("pick") { ReadmeThreadPicker(HOST, board).pick() }
        device.note("thread ${picks?.thread}, video ${picks?.video}, clean ${picks?.clean}, crude skipped ${picks?.crudeSkipped}")
        device.step("start") { start() }
        device.step("theme") { darkTheme() }
        device.step("boards") { tab("Доски", "01-boards") }
        device.step("board") {
            device.open("https://$HOST/$board/")
            device.shot("02-board")
        }
        device.step("thread") {
            openThread(picks)
            device.shot("03-thread")
        }
        device.step("favorite") { device.tap("Добавить тред в избранное") }
        device.step("viewer") { viewer() }
        device.step("video") { video(picks?.video) }
        device.step("threadshot") { threadshot() }
        device.step("threads") { tab("Треды", "07-threads") }
        device.step("composer") { composer() }
        device.step("settings") { settings() }
    }

    private fun start() {
        device.launch()
        runCatching { device.tap(device.findAny(FIRST_RUN_LABELS, SHORT_WAIT_MS)) }
    }

    private fun darkTheme() {
        device.tap("Настройки")
        device.tap("Интерфейс")
        device.tap(device.scrollTo("Режим темы"))
        device.tap("Тёмный")
        device.settle(LOAD_MS)
        device.back()
    }

    private fun tab(
        label: String,
        shot: String,
    ) {
        device.tap(label)
        device.settle(LOAD_MS)
        device.shot(shot)
    }

    /** Waits for real posts, not the loading placeholders, and lets the thumbnails arrive. */
    private fun openThread(picks: ReadmePicks?) {
        if (picks != null) device.open("https://$HOST/$board/res/${picks.thread}.html") else device.tap(thumbnail())
        device.waitFor("posts", POSTS_WAIT_MS) { DATE.containsMatchIn(device.label(it)) }
        device.settle(THUMBNAILS_MS)
    }

    private fun viewer() {
        device.tap(thumbnail())
        device.settle(VIEWER_OPEN_MS)
        device.shot("04-viewer")
        device.record("gif-viewer", VIEWER_GIF_SECONDS) {
            repeat(2) { page(forward = true) }
            page(forward = false)
            device.swipe(device.width / 2 to device.height * 45 / PERCENT, device.width / 2 to device.height * 85 / PERCENT, DISMISS_MS)
            device.settle(GIF_PAUSE_MS)
        }
    }

    /** Opens the first file again and pages exactly to the thread's first video (its place comes from the JSON). */
    private fun video(video: ReadmeVideo?) {
        checkNotNull(video) { "no video in the top threads" }
        device.tap(thumbnail())
        device.settle(VIEWER_OPEN_MS)
        repeat(video.index) { page(forward = true, pause = FAST_PAGE_MS) }
        device.settle(PLAYER_MS)
        if (!playerShown()) device.shell("input tap ${device.width / 2} ${device.height / 2}")
        device.settle(CONTROLS_MS)
        device.shot("05-video")
        device.back()
    }

    private fun page(
        forward: Boolean,
        pause: Long = GIF_PAUSE_MS,
    ) {
        val y = device.height / 2
        val edge = device.width * EDGE_PERCENT / PERCENT
        val start = device.width - edge
        if (forward) device.swipe(edge to y, start to y) else device.swipe(start to y, edge to y)
        device.settle(pause)
    }

    private fun playerShown(): Boolean = device.visible().any { device.label(it) in PLAYER_LABELS }

    private fun threadshot() {
        device.record("gif-threadshot", THREADSHOT_GIF_SECONDS) {
            device.tap("Меню треда")
            device.tap("Выделить")
            posts().forEach(device::tap)
            device.shot("06-threadshot")
            device.tap("Скриншот")
            save()
        }
        runCatching { device.tap(device.find("Отменить выделение", SHORT_WAIT_MS)) }.onFailure { device.back() }
    }

    /** The system save dialog: tap «Сохранить» and wait until it gives the screen back to the app. */
    private fun save() {
        check(device.waitUntil(LONG_WAIT_MS) { device.activePackage() != device.packageName }) { "no save dialog" }
        device.visible().firstOrNull { it.className == EDIT_TEXT }?.let { device.note("threadshot file: ${device.label(it)}") }
        repeat(SAVE_TRIES) {
            device.tap(device.findAny(SAVE_LABELS, LONG_WAIT_MS) { it.isClickable })
            if (device.waitUntil(LONG_WAIT_MS) { device.activePackage() == device.packageName }) return
        }
        device.back()
        error("the save dialog did not close")
    }

    private fun posts(): List<AccessibilityNodeInfo> {
        val headers =
            device
                .visible()
                .filter { DATE.containsMatchIn(device.label(it)) && device.bounds(it).top > device.height / TOP_BAR_PART }
                .distinctBy { device.bounds(it).top / ROW_STEP_PX }
                .take(2)
        check(headers.isNotEmpty()) { "no post headers to select" }
        return headers
    }

    /** A square clickable tile between the toolbars: post thumbnails, whatever their labels say. */
    private fun thumbnail(): AccessibilityNodeInfo =
        device.waitFor("thumbnail", LONG_WAIT_MS) { node ->
            val area = device.bounds(node)
            node.isClickable &&
                area.top > device.height / TOP_BAR_PART &&
                area.bottom < device.height - device.height / TOP_BAR_PART &&
                area.width() in device.width / TILE_MIN_PART..device.width / 2 &&
                abs(area.width() - area.height()) < area.width() / TILE_SKEW_PART
        }

    /** «Написать» needs a board behind it, so the composer opens from the board. */
    private fun composer() {
        device.open("https://$HOST/$board/")
        device.tap("Написать")
        val field = device.waitFor("message field", LONG_WAIT_MS) { it.className == EDIT_TEXT }
        device.setText(field, SAMPLE_MESSAGE)
        if (device.keyboardShown()) device.back()
        device.shot("08-composer")
        device.back()
        runCatching { device.tap(device.findAny(DRAFT_LABELS, SHORT_WAIT_MS)) }
    }

    private fun settings() {
        device.tap("Настройки")
        runCatching { device.tap(device.find("Интерфейс", SHORT_WAIT_MS)) }
        device.tap(device.scrollTo("Порядок кнопок"))
        device.shot("09-bottom-bar")
        device.back()
        device.tap(device.scrollTo("Язык интерфейса"))
        device.shot("10-language")
        device.back()
    }
}

private const val HOST = "2ch.su"
private const val DEFAULT_BOARD = "2d"
private const val EDIT_TEXT = "android.widget.EditText"
private const val SAMPLE_MESSAGE = "Привет! Это Pawchan: двач и форчан в одном клиенте."
private val FIRST_RUN_LABELS = setOf("Понятно", "Закрыть", "Позже")
private val SAVE_LABELS = setOf("Сохранить", "СОХРАНИТЬ", "Save", "SAVE")
private val DRAFT_LABELS = setOf("Сохранить", "Сохранить черновик", "Оставить")
private val PLAYER_LABELS = setOf("Пауза", "Воспроизвести")
private val DATE = Regex("\\d{2}[./]\\d{2}[./]\\d{2,4}")
private const val SHORT_WAIT_MS = 2_000L
private const val LONG_WAIT_MS = 15_000L
private const val POSTS_WAIT_MS = 45_000L
private const val FAST_PAGE_MS = 700L
private const val CONTROLS_MS = 600L
private const val THUMBNAILS_MS = 3_000L
private const val VIEWER_OPEN_MS = 1_500L
private const val PLAYER_MS = 2_500L
private const val GIF_PAUSE_MS = 1_400L
private const val VIEWER_GIF_SECONDS = 9
private const val THREADSHOT_GIF_SECONDS = 18
private const val SAVE_TRIES = 2
private const val DISMISS_MS = 180
private const val EDGE_PERCENT = 85
private const val PERCENT = 100
private const val TOP_BAR_PART = 8
private const val ROW_STEP_PX = 60
private const val TILE_MIN_PART = 7
private const val TILE_SKEW_PART = 5
