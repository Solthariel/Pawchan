package com.solthariel.pawchan.readme

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Locale
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
 * The GitHub page shows four clips, each feature once: reading (board, search, thread, its menu with the numbers,
 * files, a reply preview), the thread's gallery, a new post with its files and the picture editor, and the app's
 * sections with a run through the settings. Every step is independent: a failed one leaves a line in the log (and a
 * fail-*.png when the app is in front), the rest still run.
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
        device.note("flow 7")
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
        device.step("reading") { reading(picks) }
        device.step("favorite") { device.tap("Добавить тред в избранное") }
        device.step("gallery") { gallery(picks) }
        device.step("composer") { composer() }
        device.step("editor") { editor() }
        device.step("draft") { leaveComposer() }
        device.step("sections") { sections() }
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

    /** One reading run in the order a reader goes: the board and its search, the thread and its menu, files, a reply. */
    private fun reading(picks: ReadmePicks?) {
        device.record("gif-reading", READING_GIF_SECONDS) {
            device.settle(GIF_PAUSE_MS)
            openThread(picks)
            device.mark("thread")
            runCatching { threadMenu() }.onFailure { device.note("menu: ${it.message}") }
            device.mark("menu closed")
            watchFiles(picks?.video)
            device.mark("viewer closed")
            replyPreview()
            device.mark("end")
        }
    }

    /** The board's search finds the thread by a word of its title; the address opens it when the search cannot. */
    private fun openThread(picks: ReadmePicks?) {
        val subject = picks?.subject.orEmpty().trim()
        val found =
            subject.isNotEmpty() &&
                runCatching { searchThread(subject) }.onFailure { device.note("search: ${it.message}") }.isSuccess
        if (!found) {
            if (picks != null) device.open("https://$HOST/$board/res/${picks.thread}.html") else device.tap(thumbnail())
        }
        // Real posts, not the loading placeholders; then the thumbnails arrive.
        device.waitFor("posts", POSTS_WAIT_MS) { DATE.containsMatchIn(device.label(it)) }
        device.settle(THUMBNAILS_MS)
    }

    /** «Меню треда» right after the thread opens: its actions and the thread's numbers (posts, files, size, people). */
    private fun threadMenu() {
        device.settle(GIF_PAUSE_MS / 2)
        device.tap("Меню треда")
        val shown = device.waitUntil(SHORT_WAIT_MS) { menuShown() }
        // Back closes an open menu only; without the menu it would leave the thread.
        if (shown) {
            device.mark("menu")
            device.settle(MENU_MS)
            device.back()
        }
        device.settle(GIF_PAUSE_MS / 2)
        check(shown) { "no thread menu" }
    }

    private fun menuShown(): Boolean = device.visible().any { node -> device.parts(node).any(THREAD_MENU_LABELS::contains) }

    /** The keyboard is hidden before the result is tapped: the clip shows the list, not the owner's keyboard. */
    private fun searchThread(subject: String) {
        device.tap("Поиск по тредам")
        val field = device.waitFor("search field", LONG_WAIT_MS) { it.className == EDIT_TEXT }
        device.mark("search")
        device.setText(field, searchWord(subject))
        if (device.waitUntil(KEYBOARD_WAIT_MS) { device.keyboardShown() }) device.back()
        device.mark("keyboard hidden")
        device.settle(SEARCH_MS)
        device.tap(device.waitFor(subject, LONG_WAIT_MS) { subject in device.parts(it) && it.className != EDIT_TEXT })
    }

    /** The start of the title's longest word: «Убежище для хвостатых» is searched as «хвостат». */
    private fun searchWord(subject: String): String =
        subject
            .split(' ')
            .maxBy { it.length }
            .take(SEARCH_LETTERS)
            .lowercase(Locale.ROOT)

    /** The first video plays a few seconds, two pictures follow, a swipe down closes the viewer. */
    private fun watchFiles(video: ReadmeVideo?) {
        device.tap(thumbnail())
        repeat(video?.index ?: 0) { page(forward = true, pause = FAST_PAGE_MS) }
        device.settle(VIDEO_WATCH_MS)
        repeat(2) { page(forward = true) }
        device.swipe(device.width / 2 to device.height * 45 / PERCENT, device.width / 2 to device.height * 85 / PERCENT, DISMISS_MS)
        device.settle(GIF_PAUSE_MS / 2)
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

    /** A scroll and a reply link: the quoted post pops up over the thread. */
    private fun replyPreview() {
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
        device.settle(GIF_PAUSE_MS)
    }

    private fun scroll() {
        val x = device.width / 2
        device.swipe(x to device.height * SCROLL_FROM_PERCENT / PERCENT, x to device.height * SCROLL_TO_PERCENT / PERCENT, SCROLL_SWIPE_MS)
        device.settle(GIF_PAUSE_MS / 2)
    }

    /** A square clickable tile between the toolbars: post thumbnails, whatever their labels say. */
    private fun thumbnail(): AccessibilityNodeInfo = device.waitFor("thumbnail", LONG_WAIT_MS, ::isTile)

    private fun isTile(node: AccessibilityNodeInfo): Boolean {
        val area = device.bounds(node)
        return node.isClickable &&
            area.top > device.height / TOP_BAR_PART &&
            area.bottom < device.height - device.height / TOP_BAR_PART &&
            area.width() in device.width / TILE_MIN_PART..device.width / 2 &&
            abs(area.width() - area.height()) < area.width() / TILE_SKEW_PART
    }

    /** All the thread's files in one grid: two scrolls, the video filter, then three files picked for the bar. */
    private fun gallery(picks: ReadmePicks?) {
        if (device.visible().none { "Галерея" in device.parts(it) }) {
            checkNotNull(picks) { "no thread for the gallery" }
            device.open("https://$HOST/$board/res/${picks.thread}.html")
            device.waitFor("posts", POSTS_WAIT_MS) { DATE.containsMatchIn(device.label(it)) }
            device.settle(THUMBNAILS_MS)
        }
        device.record("gif-gallery", GALLERY_GIF_SECONDS) {
            device.settle(GIF_PAUSE_MS / 2)
            device.tap("Галерея")
            galleryTiles(LONG_WAIT_MS)
            device.mark("grid")
            device.settle(GALLERY_LOAD_MS)
            repeat(2) { scroll() }
            runCatching { videoFilter() }.onFailure { device.note("gallery filter: ${it.message}") }
            runCatching { pickFiles() }.onFailure { device.note("gallery picks: ${it.message}") }
            // The gallery is a window of its own: Back closes all of it.
            device.back()
            device.settle(GIF_PAUSE_MS)
        }
    }

    /** Only the thread's videos, then all files again. */
    private fun videoFilter() {
        device.tap(galleryFilter("Видео"))
        device.mark("videos")
        device.settle(GIF_PAUSE_MS)
        device.tap(galleryFilter("Все"))
        device.settle(GIF_PAUSE_MS / 2)
    }

    /** A filter button says «Видео: 12»: its name and how many files it keeps. */
    private fun galleryFilter(name: String): AccessibilityNodeInfo =
        device.waitFor("$name filter", SHORT_WAIT_MS) { node -> device.parts(node).any { it.startsWith("$name: ") } }

    /** Three files of the top row picked: the bar below offers to download them, copy their links or keep them. */
    private fun pickFiles() {
        device.tap("Выделить медиа")
        galleryTiles(SHORT_WAIT_MS).take(GALLERY_PICKS).forEach(device::tap)
        device.mark("picked")
        device.settle(GIF_PAUSE_MS)
        device.tap(device.find("Отмена", SHORT_WAIT_MS))
        device.settle(GIF_PAUSE_MS / 2)
    }

    /** The gallery's own cells, top row first; the thread under its window has square thumbnails as well. */
    private fun galleryTiles(timeoutMs: Long): List<AccessibilityNodeInfo> {
        var tiles = emptyList<AccessibilityNodeInfo>()
        device.waitUntil(timeoutMs) {
            tiles =
                device
                    .front()
                    .filter(::isTile)
                    .sortedWith(compareBy({ device.bounds(it).top }, { device.bounds(it).left }))
            tiles.isNotEmpty()
        }
        check(tiles.isNotEmpty()) { "no gallery tiles" }
        return tiles
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

    /**
     * The composer with its files, then the first still picture in the editor: a square crop, a pen line, an oval and
     * a pixelated box; Back takes the edits to the post, so the clip ends on the composer with the edited tile.
     */
    private fun editor() {
        val tile = pictureTile()
        device.record("gif-editor", EDITOR_GIF_SECONDS) {
            device.settle(GIF_PAUSE_MS)
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
            device.back()
            device.settle(GIF_PAUSE_MS)
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

    /** Out of the composer, the viewer first if a failed step left it open; the draft stays with the wiped copy. */
    private fun leaveComposer() {
        repeat(LEAVE_TRIES) {
            if (device.visible().any { "Написать" in device.parts(it) }) return
            device.back()
            runCatching { device.tap(device.findAny(DRAFT_LABELS, SHORT_WAIT_MS)) }
        }
    }

    /** The bottom bar's sections with the 2ch and 4chan boards, then a quick look through the settings. */
    private fun sections() {
        bar("Каталог")
        device.settle(LOAD_MS)
        device.record("gif-sections", SECTIONS_GIF_SECONDS) {
            device.settle(TAB_MS)
            bar("Треды")
            device.settle(TAB_MS)
            bar("Доски")
            device.settle(TAB_MS)
            runCatching { sources() }.onFailure { device.note("sources: ${it.message}") }
            bar("Настройки")
            device.settle(TAB_MS)
            device.mark("settings")
            SETTINGS_TOUR.forEach { section ->
                runCatching {
                    device.tap(device.scrollTo(section))
                    device.settle(SECTION_MS)
                }.onFailure { device.note("section $section: ${it.message}") }
                backToSettings()
            }
            device.settle(TAB_MS)
        }
    }

    /** 4chan's boards by one creative category, since its full list starts with the adult ones; then 2ch again. */
    private fun sources() {
        device.tap(device.find("4chan", SHORT_WAIT_MS))
        device.mark("4chan")
        device.settle(CHIP_MS)
        runCatching { device.tap(device.find("Творчество", SHORT_WAIT_MS)) }
        device.mark("4chan creative")
        device.settle(TAB_MS)
        device.tap(device.find("2ch", SHORT_WAIT_MS))
        device.settle(TAB_MS / 2)
    }

    /** Back from a section (a dialog on top of it included) until the settings list shows again. */
    private fun backToSettings() {
        repeat(LEAVE_TRIES) {
            if (onSettingsList()) return
            device.back()
            device.waitUntil(SHORT_WAIT_MS) { onSettingsList() }
        }
    }

    /** The list shows most section titles at once; a single section shows one of them at most. */
    private fun onSettingsList(): Boolean {
        val labels = device.visible().flatMap(device::parts).toSet()
        return SETTINGS_TITLES.count { it in labels } >= SETTINGS_LIST_MIN
    }
}

private const val HOST = "2ch.su"
private const val DEFAULT_BOARD = "2d"
private const val EDIT_TEXT = "android.widget.EditText"
private const val SAMPLE_MESSAGE = "Привет! Это Pawchan: двач и форчан в одном клиенте."
private val FIRST_RUN_LABELS = setOf("Понятно", "Закрыть", "Позже")
private val THREAD_MENU_LABELS = setOf("Участников", "Расширенный поиск")
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
private const val VIDEO_WATCH_MS = 3_500L
private const val GIF_PAUSE_MS = 1_400L
private const val PREVIEW_MS = 2_200L
private const val MENU_MS = 2_200L

// Long enough for the system's «pasted from clipboard» notice to fade before the frame.
private const val ATTACH_MS = 4_500L
private const val EDITOR_MS = 1_500L
private const val EDIT_PAUSE_MS = 900L
private const val THEME_MS = 1_600L
private const val TAB_MS = 1_300L
private const val EDITOR_GIF_SECONDS = 30
private const val READING_GIF_SECONDS = 60
private const val GALLERY_GIF_SECONDS = 45
private const val GALLERY_LOAD_MS = 1_800L
private const val GALLERY_PICKS = 3
private const val SECTIONS_GIF_SECONDS = 40
private const val SEARCH_MS = 1_200L
private const val KEYBOARD_WAIT_MS = 1_500L
private const val SEARCH_LETTERS = 7
private const val CHIP_MS = 500L
private const val SECTION_MS = 1_300L
private const val LEAVE_TRIES = 3
private const val SETTINGS_LIST_MIN = 4
private val SETTINGS_TOUR = listOf("Интерфейс", "Медиа", "Автоскрытие", "Сеть", "О программе")
private val SETTINGS_TITLES =
    setOf("Общие", "Интерфейс", "Треды и каталог", "Медиа", "Отправка", "Автоскрытие", "Сеть", "Хранилище", "О программе")
private const val DISMISS_MS = 180
private const val STROKE_MS = 700
private const val SCROLL_SWIPE_MS = 650
private const val EDGE_PERCENT = 85
private const val PERCENT = 100
private const val BAR_TOP_PERCENT = 85
private const val SCROLL_FROM_PERCENT = 72
private const val SCROLL_TO_PERCENT = 40
private const val TOP_BAR_PART = 8
private const val LINK_X_PX = 40
private const val LINK_Y_PX = 25
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
private const val BOX_LEFT = 10.0
private const val BOX_TOP = 36.0
private const val BOX_RIGHT = 44.0
private const val BOX_BOTTOM = 47.0
