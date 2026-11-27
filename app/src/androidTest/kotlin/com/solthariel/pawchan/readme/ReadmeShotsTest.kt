package com.solthariel.pawchan.readme

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Screens for the GitHub page, filmed on a live board. It runs only from scripts/readme-shots.ps1, which installs a
 * release-speed copy under its own package, wipes it and passes readmeShots=1, so the regular device smoke skips it.
 */
class ReadmeShotsTest {
    @Test
    fun filmReadmeScreens() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Only for scripts/readme-shots.ps1", arguments.getString("readmeShots") == "1")
        val target = arguments.getString("target") ?: InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val device = ReadmeDevice(target)
        device.prepare()
        try {
            val board = arguments.getString("board") ?: DEFAULT_BOARD
            val attach =
                arguments
                    .getString("attach")
                    .orEmpty()
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
            ReadmeFlow(device, board, arguments.getString("thread")?.toLongOrNull(), attach).run()
        } finally {
            device.clearPictures()
            device.finish()
        }
    }
}

/**
 * The GitHub page shows each feature once: clips for what moves, single frames for the rest. Every step is
 * independent: a failed one leaves a line in the log (and a fail-*.png when the app is in front), the rest still run.
 */
internal class ReadmeFlow(
    private val device: ReadmeDevice,
    private val board: String,
    private val thread: Long?,
    private val attach: List<String>,
) {
    /** The composer's files as shared storage named them, in the order they are attached. */
    private var files: List<String> = emptyList()

    fun run() {
        device.note("flow 4")
        val picker = ReadmeThreadPicker(HOST, board)
        val picks = device.step("pick") { thread?.let(picker::thread) ?: picker.pick() }
        device.note("thread ${picks?.thread}, video ${picks?.video}, clean ${picks?.clean}, crude skipped ${picks?.crudeSkipped}")
        device.step("files") { files = composerFiles(picker, picks) }
        device.step("start") { start() }
        device.step("theme") { darkTheme() }
        device.step("board") {
            device.open("https://$HOST/$board/")
            device.shot("02-board")
        }
        device.step("thread") { openThread(picks) }
        device.step("stats") { stats() }
        device.step("favorite") { device.tap("Добавить тред в избранное") }
        device.step("viewer") { viewer(picks?.video) }
        device.step("threadshot") { threadshot() }
        device.step("replies") { replies() }
        device.step("composer") { composer() }
        device.step("editor") { editor() }
        device.step("draft") { leaveComposer() }
        device.step("themes") { themes() }
        device.step("tabs") { tabs() }
    }

    /** The files the owner passed (attach=url,url,...), else light pictures of the filmed thread. */
    private fun composerFiles(
        picker: ReadmeThreadPicker,
        picks: ReadmePicks?,
    ): List<String> {
        val chosen = if (attach.isNotEmpty()) readmeAttachments(attach) else checkNotNull(picks) { "no thread" }.pictures
        check(chosen.isNotEmpty()) { "no files for the composer" }
        val names = device.addPictures(chosen.map { it.name to picker.bytes(it.url) })
        device.note("files ${names.joinToString()}")
        return names
    }

    private fun start() {
        device.launch()
        runCatching { device.tap(device.findAny(FIRST_RUN_LABELS, SHORT_WAIT_MS)) }
    }

    private fun darkTheme() {
        interfaceSettings()
        chooseTheme("Тёмный")
        device.back()
    }

    private fun interfaceSettings() {
        bar("Настройки")
        runCatching { device.tap(device.find("Интерфейс", SHORT_WAIT_MS)) }
    }

    private fun chooseTheme(mode: String) {
        device.tap(device.scrollTo("Режим темы"))
        device.tap(mode)
        device.settle(THEME_MS)
    }

    /** A button of the bottom bar, not a title with the same word. */
    private fun bar(label: String) {
        val button =
            device.waitFor(label, LONG_WAIT_MS) {
                label in device.label(it).split(", ") && device.bounds(it).top > device.height * BAR_TOP_PERCENT / PERCENT
            }
        device.tap(button)
    }

    /** Waits for real posts, not the loading placeholders, and lets the thumbnails arrive. */
    private fun openThread(picks: ReadmePicks?) {
        if (picks != null) device.open("https://$HOST/$board/res/${picks.thread}.html") else device.tap(thumbnail())
        device.waitFor("posts", POSTS_WAIT_MS) { DATE.containsMatchIn(device.label(it)) }
        device.settle(THUMBNAILS_MS)
    }

    /** The thread menu opens with the thread's numbers: posts, files, their size and participants. */
    private fun stats() {
        device.tap("Меню треда")
        device.settle(MENU_MS)
        device.shot("08-stats")
        device.back()
    }

    /** The first video plays a few seconds, two pictures follow, a swipe down closes the viewer. */
    private fun viewer(video: ReadmeVideo?) {
        device.record("gif-viewer", VIEWER_GIF_SECONDS) {
            device.tap(thumbnail())
            repeat(video?.index ?: 0) { page(forward = true, pause = FAST_PAGE_MS) }
            device.settle(VIDEO_WATCH_MS)
            repeat(2) { page(forward = true) }
            device.swipe(device.width / 2 to device.height * 45 / PERCENT, device.width / 2 to device.height * 85 / PERCENT, DISMISS_MS)
            device.settle(GIF_PAUSE_MS)
        }
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

    /** Two posts become one picture; the system save dialog is cut out of the clip afterwards. */
    private fun threadshot() {
        device.record("gif-threadshot", THREADSHOT_GIF_SECONDS) {
            device.tap("Меню треда")
            device.tap("Выделить")
            selectedPosts().forEach(device::tap)
            device.shot("06-threadshot")
            device.tap("Скриншот")
            save()
            device.settle(NOTICE_MS)
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

    /** The 4th and 5th posts when they are on screen (in the chosen thread a picture and its reply), else the first two. */
    private fun selectedPosts(): List<AccessibilityNodeInfo> {
        val headers =
            device
                .visible()
                .filter { DATE.containsMatchIn(device.label(it)) }
                .filter { device.bounds(it).top > device.height / TOP_BAR_PART }
                .filter { device.bounds(it).bottom < device.height * BAR_TOP_PERCENT / PERCENT }
                .distinctBy { device.bounds(it).top / ROW_STEP_PX }
                .sortedBy { device.bounds(it).top }
        check(headers.isNotEmpty()) { "no post headers to select" }
        return if (headers.size >= PAIR_END) headers.subList(PAIR_START, PAIR_END) else headers.take(2)
    }

    /** Scrolls the thread and opens a reply link: the quoted post pops up over the thread. */
    private fun replies() {
        device.record("gif-thread", THREAD_GIF_SECONDS) {
            device.settle(GIF_PAUSE_MS)
            scroll()
            val link =
                device.waitFor("reply link", LONG_WAIT_MS) { node ->
                    val area = device.bounds(node)
                    REPLY.containsMatchIn(device.label(node)) &&
                        area.top > device.height / TOP_BAR_PART &&
                        area.bottom < device.height * BAR_TOP_PERCENT / PERCENT
                }
            // The link opens the message, so the tap goes on its first characters.
            val area = device.bounds(link)
            device.shell("input tap ${area.left + LINK_X_PX} ${area.top + LINK_Y_PX}")
            device.settle(PREVIEW_MS)
            device.back()
            device.settle(GIF_PAUSE_MS / 2)
            scroll()
            device.settle(GIF_PAUSE_MS)
        }
    }

    private fun scroll() {
        val x = device.width / 2
        device.swipe(x to device.height * SCROLL_FROM_PERCENT / PERCENT, x to device.height * SCROLL_TO_PERCENT / PERCENT, SCROLL_SWIPE_MS)
        device.settle(GIF_PAUSE_MS / 2)
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

    /** «Написать» needs a board behind it; the files go in as one paste, the system file picker is the fallback. */
    private fun composer() {
        // The clip is made before the board opens, so the system's «copied» notice is gone before any frame.
        if (files.isNotEmpty()) runCatching { device.copyPictures() }.onFailure { device.note("clip: ${it.message}") }
        device.open("https://$HOST/$board/")
        bar("Написать")
        device.setText(messageField(), SAMPLE_MESSAGE)
        if (device.keyboardShown()) device.back()
        if (files.isNotEmpty()) device.step("attach") { attach() }
        if (device.keyboardShown()) device.back()
        device.settle(ATTACH_MS)
        device.shot("05-composer")
    }

    private fun messageField(): AccessibilityNodeInfo = device.waitFor("message field", LONG_WAIT_MS) { it.className == EDIT_TEXT }

    /** The composer reads pasted media like picked ones; only when nothing arrives the system file picker opens. */
    private fun attach() {
        val pasted = runCatching { paste() }.onFailure { device.note("paste: ${it.message}") }.getOrDefault(false)
        if (!pasted && attachedCount() == 0) {
            runCatching { pick(files) }.onFailure { error ->
                device.note("picker: ${error.message}")
                repeat(PICKER_BACKS) { if (device.activePackage() != device.packageName) device.back() }
            }
        }
        val count = attachedCount(IMPORT_WAIT_MS)
        // A paste that the field took as text leaves the files' links in the message.
        val field = messageField()
        if (field.text?.toString() != SAMPLE_MESSAGE) device.setText(field, SAMPLE_MESSAGE)
        device.note("attached $count of ${files.size}")
        check(count > 0) { "no attachment tiles" }
    }

    /** Pastes into the message field until the tiles show; false when no try brought a file. */
    private fun paste(): Boolean {
        repeat(PASTE_TRIES) { attempt ->
            val accepted = messageField().performAction(AccessibilityNodeInfo.ACTION_PASTE)
            if (!accepted) {
                // Without the accessibility action the paste key of a focused field does the same.
                messageField().performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                device.shell("input keyevent $KEYCODE_PASTE")
            }
            device.note("paste ${attempt + 1}: ${if (accepted) "action" else "key"}")
            if (attachedCount(IMPORT_WAIT_MS) > 0) return true
            if (device.keyboardShown()) device.back()
            // Posting rules may still be loading: the composer refuses files until they arrive.
            device.settle(PASTE_RETRY_MS)
        }
        return false
    }

    /** Files on the composer's tiles («Вложение N из M», or their names); waits up to [timeoutMs] for all of them. */
    private fun attachedCount(timeoutMs: Long = 0): Int {
        var count = 0
        device.waitUntil(timeoutMs) {
            val nodes = device.visible()
            val byState = nodes.maxOfOrNull(::attachmentTotal) ?: 0
            val byName = files.count { name -> nodes.any { name in device.parts(it) } }
            count = maxOf(byState, byName)
            count >= files.size
        }
        return count
    }

    /** M of a tile's «Вложение N из M»; 0 for any other node. */
    private fun attachmentTotal(node: AccessibilityNodeInfo): Int {
        val match = ATTACHMENT.find(device.state(node)) ?: return 0
        return match.groupValues[2].toInt()
    }

    /** Picks the prepared files; the picker's own screens never go into a frame or a clip. */
    private fun pick(names: List<String>) {
        device.tap("Добавить медиа")
        check(device.waitUntil(LONG_WAIT_MS) { device.activePackage() != device.packageName }) { "no file picker" }
        device.note("picker ${device.activePackage()}")
        if (device.activePackage() in RESOLVER_PACKAGES) {
            device.tap(device.findAny(CHOOSER_LABELS, LONG_WAIT_MS))
            runCatching { device.tap(device.findAny(ONCE_LABELS, SHORT_WAIT_MS)) }
        }
        device.longPress(picture(names.first()))
        names.drop(1).forEach { device.tap(device.find(it)) }
        device.tap(device.findAny(PICK_LABELS, LONG_WAIT_MS))
        check(device.waitUntil(LONG_WAIT_MS) { device.activePackage() == device.packageName }) { "the file picker did not close" }
    }

    /** Recent files usually list the new files first; otherwise their folder is in Downloads. */
    private fun picture(name: String): AccessibilityNodeInfo =
        runCatching { device.find(name, PICKER_WAIT_MS) }.getOrElse {
            device.tap(device.findAny(ROOTS_LABELS, SHORT_WAIT_MS))
            device.tap(device.findAny(DOWNLOADS_LABELS, LONG_WAIT_MS))
            device.tap(device.find(PICTURES_FOLDER, LONG_WAIT_MS))
            device.find(name, LONG_WAIT_MS)
        }

    /** The first still picture in the attachment editor: a square crop, a pen line, an oval and a pixelated box. */
    private fun editor() {
        val tile = pictureTile()
        device.record("gif-editor", EDITOR_GIF_SECONDS) {
            device.tap(tile)
            device.settle(EDITOR_MS)
            device.tap("Изменить")
            device.tap("1:1")
            device.settle(EDIT_PAUSE_MS)
            device.tap("Рисовать")
            device.draw(wave())
            device.tap("Овал")
            device.swipe(at(OVAL_LEFT, OVAL_TOP), at(OVAL_RIGHT, OVAL_BOTTOM), STROKE_MS)
            device.settle(EDIT_PAUSE_MS)
            device.tap("Замазать")
            runCatching { device.tap(device.find("Рамкой", SHORT_WAIT_MS)) }
            runCatching { device.tap(device.find("Пиксели", SHORT_WAIT_MS)) }
            device.swipe(at(BOX_LEFT, BOX_TOP), at(BOX_RIGHT, BOX_BOTTOM), STROKE_MS)
            device.settle(EDIT_PAUSE_MS)
            device.tap("Назад к файлу")
            device.settle(EDIT_PAUSE_MS)
        }
    }

    /** The editor opens stills only: the first picture's tile by its name, its spoken position or its place in the row. */
    private fun pictureTile(): AccessibilityNodeInfo {
        val index = files.indexOfFirst { !isVideo(it) }.coerceAtLeast(0)
        val byName = files.getOrNull(index)?.let { runCatching { device.find(it, SHORT_WAIT_MS) }.getOrNull() }
        if (byName != null) return byName
        val position = "Вложение ${index + 1} из ${files.size}"
        device.visible().firstOrNull { device.state(it) == position }?.let { return it }
        val tiles =
            device
                .visible()
                .filter { node ->
                    val area = device.bounds(node)
                    node.isClickable &&
                        area.top > device.height / 2 &&
                        area.width() in device.width / ATTACH_TILE_MIN_PART..device.width / ATTACH_TILE_MAX_PART &&
                        abs(area.width() - area.height()) < area.width() / TILE_SKEW_PART
                }.sortedBy { device.bounds(it).left }
        return tiles.getOrNull(index) ?: tiles.firstOrNull() ?: error("no attachment tile")
    }

    /** A gentle pen wave across the upper middle of the picture. */
    private fun wave(): List<Pair<Int, Int>> =
        (0..WAVE_POINTS).map { point ->
            val part = point.toDouble() / WAVE_POINTS
            val x = WAVE_LEFT + (WAVE_RIGHT - WAVE_LEFT) * part
            at(x, WAVE_MIDDLE + WAVE_HEIGHT * sin(part * WAVE_TURNS))
        }

    /** A point given in percent of the screen. */
    private fun at(
        xPercent: Double,
        yPercent: Double,
    ): Pair<Int, Int> = (device.width * xPercent / PERCENT).toInt() to (device.height * yPercent / PERCENT).toInt()

    /** Out of the editor and the composer; the draft stays with the filmed copy, which is wiped afterwards. */
    private fun leaveComposer() {
        device.back()
        device.back()
        runCatching { device.tap(device.findAny(DRAFT_LABELS, SHORT_WAIT_MS)) }
    }

    /** The theme changes live: light, AMOLED and back to dark. */
    private fun themes() {
        interfaceSettings()
        device.scrollTo("Режим темы")
        device.record("gif-themes", THEMES_GIF_SECONDS) {
            device.settle(GIF_PAUSE_MS / 2)
            listOf("Светлый", "AMOLED", "Тёмный").forEach(::chooseTheme)
        }
        device.back()
    }

    /** The bottom bar in order: catalog, threads, boards with the 2ch/4chan switch, settings. */
    private fun tabs() {
        bar("Каталог")
        device.settle(LOAD_MS)
        device.record("gif-tabs", TABS_GIF_SECONDS) {
            device.settle(TAB_MS)
            bar("Треды")
            device.settle(TAB_MS)
            bar("Доски")
            device.settle(TAB_MS)
            runCatching {
                device.tap(device.find("4chan", SHORT_WAIT_MS))
                device.settle(TAB_MS)
                device.tap(device.find("2ch", SHORT_WAIT_MS))
                device.settle(TAB_MS / 2)
            }
            bar("Настройки")
            device.settle(TAB_MS)
            bar("Каталог")
            device.settle(TAB_MS)
        }
    }
}

private const val HOST = "2ch.su"
private const val DEFAULT_BOARD = "2d"
private const val EDIT_TEXT = "android.widget.EditText"
private const val SAMPLE_MESSAGE = "Привет! Это Pawchan: двач и форчан в одном клиенте."
private val FIRST_RUN_LABELS = setOf("Понятно", "Закрыть", "Позже")
private val SAVE_LABELS = setOf("Сохранить", "СОХРАНИТЬ", "Save", "SAVE")
private val DRAFT_LABELS = setOf("Сохранить", "Сохранить черновик", "Оставить")
private val RESOLVER_PACKAGES = setOf("android", "com.android.intentresolver")
private val CHOOSER_LABELS = setOf("Файлы", "Files")
private val ONCE_LABELS = setOf("Только сейчас", "Один раз", "Just once")
private val ROOTS_LABELS = setOf("Показать корневые папки", "Показать корни", "Show roots")
private val DOWNLOADS_LABELS = setOf("Скачанные", "Загрузки", "Downloads")
private val PICK_LABELS = setOf("Выбрать", "ВЫБРАТЬ", "Открыть", "ОТКРЫТЬ", "Готово", "ГОТОВО", "Select", "SELECT", "Open", "OPEN", "Done")
private val DATE = Regex("\\d{2}[./]\\d{2}[./]\\d{2,4}")
private val REPLY = Regex(">>\\d{5,}")
private val ATTACHMENT = Regex("Вложение (\\d+) из (\\d+)")
private const val SHORT_WAIT_MS = 2_000L
private const val LONG_WAIT_MS = 15_000L
private const val POSTS_WAIT_MS = 45_000L
private const val PICKER_WAIT_MS = 6_000L
private const val IMPORT_WAIT_MS = 20_000L
private const val PASTE_RETRY_MS = 3_000L
private const val PASTE_TRIES = 3
private const val PICKER_BACKS = 3
private const val KEYCODE_PASTE = 279
private const val FAST_PAGE_MS = 700L
private const val THUMBNAILS_MS = 3_000L
private const val MENU_MS = 1_000L
private const val VIDEO_WATCH_MS = 4_500L
private const val GIF_PAUSE_MS = 1_400L
private const val NOTICE_MS = 2_500L
private const val PREVIEW_MS = 2_200L

// Long enough for the system's «pasted from clipboard» notice to fade before the frame.
private const val ATTACH_MS = 4_500L
private const val EDITOR_MS = 1_500L
private const val EDIT_PAUSE_MS = 900L
private const val THEME_MS = 1_600L
private const val TAB_MS = 1_300L
private const val VIEWER_GIF_SECONDS = 16
private const val THREADSHOT_GIF_SECONDS = 20
private const val THREAD_GIF_SECONDS = 14
private const val EDITOR_GIF_SECONDS = 24
private const val THEMES_GIF_SECONDS = 16
private const val TABS_GIF_SECONDS = 16
private const val SAVE_TRIES = 2
private const val DISMISS_MS = 180
private const val STROKE_MS = 700
private const val SCROLL_SWIPE_MS = 650
private const val EDGE_PERCENT = 85
private const val PERCENT = 100
private const val BAR_TOP_PERCENT = 85
private const val SCROLL_FROM_PERCENT = 72
private const val SCROLL_TO_PERCENT = 40
private const val TOP_BAR_PART = 8
private const val ROW_STEP_PX = 60
private const val LINK_X_PX = 40
private const val LINK_Y_PX = 25
private const val PAIR_START = 3
private const val PAIR_END = 5
private const val TILE_MIN_PART = 7
private const val TILE_SKEW_PART = 5
private const val ATTACH_TILE_MIN_PART = 6
private const val ATTACH_TILE_MAX_PART = 3
private const val WAVE_POINTS = 16
private const val WAVE_LEFT = 30.0
private const val WAVE_RIGHT = 70.0
private const val WAVE_MIDDLE = 30.0
private const val WAVE_HEIGHT = 3.0
private const val WAVE_TURNS = 2 * PI
private const val OVAL_LEFT = 38.0
private const val OVAL_TOP = 20.0
private const val OVAL_RIGHT = 62.0
private const val OVAL_BOTTOM = 34.0
private const val BOX_LEFT = 30.0
private const val BOX_TOP = 38.0
private const val BOX_RIGHT = 55.0
private const val BOX_BOTTOM = 46.0
