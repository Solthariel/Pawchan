package com.solthariel.pawchan.readme

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale

/**
 * Drives an installed app like a person would: shell taps and swipes, screen nodes found through accessibility.
 * The filmed app is [packageName]; it can be another install than the one this instrumentation runs in.
 */
internal class ReadmeDevice(
    val packageName: String,
) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation = instrumentation.uiAutomation
    private val report = StringBuilder()
    private val pictures = mutableListOf<Uri>()
    private var clipped = false
    private var clip = ""
    private var clipStart = 0L
    val width: Int
    val height: Int

    init {
        automation.serviceInfo =
            automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        val size = Regex("(\\d+)x(\\d+)").findAll(shell("wm size")).lastOrNull()?.destructured
        width = size?.component1()?.toInt() ?: DEFAULT_WIDTH
        height = size?.component2()?.toInt() ?: DEFAULT_HEIGHT
    }

    fun prepare() {
        shell("rm -rf $SHOTS_DIR")
        shell("mkdir -p $SHOTS_DIR")
        note("screen ${width}x$height")
    }

    fun <T> step(
        name: String,
        block: () -> T,
    ): T? =
        try {
            block().also { note("OK   $name") }
        } catch (error: Throwable) {
            note("FAIL $name: ${error.message}")
            // A system picker shows the owner's own files: only the filmed app may go into a failure picture.
            repeat(BACK_TRIES) { if (activePackage() != packageName) back() }
            if (activePackage() == packageName) shell("screencap -p $SHOTS_DIR/fail-${name.replace(Regex("[^A-Za-z0-9-]"), "-")}.png")
            null
        }

    /**
     * Puts the files for the composer into Download/[PICTURES_FOLDER], where the system file picker finds them, and
     * returns the names they really got (MediaStore renames a clash instead of overwriting).
     */
    fun addPictures(files: List<Pair<String, ByteArray>>): List<String> {
        val resolver = instrumentation.targetContext.contentResolver
        // Rows a stopped run left behind; only this app's own rows match.
        runCatching {
            resolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                arrayOf(picturesPath()),
            )
        }
        return files.map { (name, bytes) ->
            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType(name))
                    put(MediaStore.MediaColumns.RELATIVE_PATH, picturesPath())
                }
            val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) { "no place for $name" }
            pictures += uri
            checkNotNull(resolver.openOutputStream(uri)) { "cannot write $name" }.use { it.write(bytes) }
            displayName(resolver, uri) ?: name
        }
    }

    /**
     * The prepared files as one clip: the composer takes pasted media like the system picker gives them, without
     * the picker's own screens. The clip is cleared again in [clearPictures].
     */
    fun copyPictures() {
        val resolver = instrumentation.targetContext.contentResolver
        val clip = ClipData.newUri(resolver, CLIP_LABEL, pictures.first())
        pictures.drop(1).forEach { clip.addItem(resolver, ClipData.Item(it)) }
        clipboard().setPrimaryClip(clip)
        clipped = true
    }

    fun clearPictures() {
        val resolver = instrumentation.targetContext.contentResolver
        if (clipped) runCatching { clipboard().clearPrimaryClip() }
        clipped = false
        pictures.forEach { runCatching { resolver.delete(it, null, null) } }
        pictures.clear()
    }

    private fun clipboard(): ClipboardManager =
        checkNotNull(instrumentation.targetContext.getSystemService(ClipboardManager::class.java)) { "no clipboard" }

    private fun picturesPath(): String = "${Environment.DIRECTORY_DOWNLOADS}/$PICTURES_FOLDER/"

    private fun displayName(
        resolver: ContentResolver,
        uri: Uri,
    ): String? =
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    fun finish() {
        File(instrumentation.targetContext.getExternalFilesDir(null), LOG_NAME).writeText(report.toString())
    }

    fun note(line: String) {
        Log.i(TAG, line)
        report.appendLine(line)
    }

    fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use {
            it.readBytes().decodeToString()
        }

    fun launch() {
        shell("am start -W -n $packageName/com.solthariel.pawchan.MainActivity")
        settle(LOAD_MS)
    }

    fun open(url: String) {
        shell("am start -W -a android.intent.action.VIEW -d $url $packageName")
        settle(LOAD_MS)
    }

    fun shot(name: String) {
        settle(SHOT_SETTLE_MS)
        shell("screencap -p $SHOTS_DIR/$name.png")
        note("shot $name")
    }

    /** Records while [actions] run; screenrecord stops by itself after [seconds]. */
    fun record(
        name: String,
        seconds: Int,
        actions: () -> Unit,
    ) {
        val size = "${width * 2 / 3 / 2 * 2}x${height * 2 / 3 / 2 * 2}"
        val recording = automation.executeShellCommand("screenrecord --time-limit $seconds --size $size $SHOTS_DIR/$name.mp4")
        clip = name
        clipStart = SystemClock.uptimeMillis()
        settle(RECORD_WARMUP_MS)
        try {
            actions()
        } finally {
            // The clip ends with its actions, not at the time limit; like Ctrl+C, SIGINT lets screenrecord close the file.
            settle(RECORD_TAIL_MS)
            runCatching { shell("pkill -INT screenrecord") }
            ParcelFileDescriptor.AutoCloseInputStream(recording).use { it.readBytes() }
            val seconds = (SystemClock.uptimeMillis() - clipStart) / MILLIS_PER_SECOND
            note("video $name ${"%.1f".format(Locale.ROOT, seconds)} s")
        }
    }

    /** A point of the running clip in the log, seconds from its start, so cuts can be made at the right frames. */
    fun mark(label: String) {
        val seconds = (SystemClock.uptimeMillis() - clipStart) / MILLIS_PER_SECOND
        note("mark $clip ${"%.2f".format(Locale.ROOT, seconds)} $label")
    }

    fun settle(millis: Long = SETTLE_MS) = SystemClock.sleep(millis)

    fun back() {
        shell("input keyevent 4")
        settle()
    }

    fun tap(label: String) = tap(find(label))

    fun tap(node: AccessibilityNodeInfo) {
        val area = bounds(node)
        shell("input tap ${area.centerX()} ${area.centerY()}")
        settle()
    }

    fun longPress(node: AccessibilityNodeInfo) {
        val area = bounds(node)
        shell("input swipe ${area.centerX()} ${area.centerY()} ${area.centerX()} ${area.centerY()} $LONG_PRESS_MS")
        settle()
    }

    /** One finger along [points], like a pen: press, move through every point, lift. */
    fun draw(points: List<Pair<Int, Int>>) {
        val (startX, startY) = points.first()
        shell("input motionevent DOWN $startX $startY")
        points.drop(1).forEach { (x, y) -> shell("input motionevent MOVE $x $y") }
        val (endX, endY) = points.last()
        shell("input motionevent UP $endX $endY")
        settle()
    }

    fun swipe(
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
        millis: Int = SWIPE_MS,
    ) {
        shell("input swipe ${from.first} ${from.second} ${to.first} ${to.second} $millis")
    }

    fun setText(
        node: AccessibilityNodeInfo,
        text: String,
    ) {
        val arguments = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        settle()
    }

    fun keyboardShown(): Boolean = automation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }

    fun bounds(node: AccessibilityNodeInfo): Rect = Rect().also(node::getBoundsInScreen)

    fun find(
        label: String,
        timeoutMs: Long = FIND_TIMEOUT_MS,
    ): AccessibilityNodeInfo = waitFor(label, timeoutMs) { label in parts(it) }

    fun findAny(
        labels: Set<String>,
        timeoutMs: Long = FIND_TIMEOUT_MS,
        also: (AccessibilityNodeInfo) -> Boolean = { true },
    ): AccessibilityNodeInfo = waitFor(labels.joinToString(), timeoutMs) { node -> parts(node).any { it in labels } && also(node) }

    /** Package of the window that has focus: the filmed app, or e.g. the system save dialog. */
    fun activePackage(): String? = automation.rootInActiveWindow?.packageName?.toString()

    fun waitUntil(
        timeoutMs: Long,
        condition: () -> Boolean,
    ): Boolean {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (!condition()) {
            if (SystemClock.uptimeMillis() >= end) return false
            SystemClock.sleep(POLL_MS)
        }
        return true
    }

    /** Scrolls the biggest list with accessibility actions (no gestures, so no slider gets dragged). */
    fun scrollTo(label: String): AccessibilityNodeInfo {
        for (action in listOf(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
            for (attempt in 0 until SCROLL_TRIES) {
                visible().firstOrNull { label in parts(it) }?.let { return it }
                val list = visible().filter { it.isScrollable }.maxByOrNull { bounds(it).height() } ?: break
                if (!list.performAction(action)) break
                settle()
            }
        }
        return find(label, SHORT_TIMEOUT_MS)
    }

    fun waitFor(
        what: String,
        timeoutMs: Long,
        match: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            visible().firstOrNull(match)?.let { return it }
            check(SystemClock.uptimeMillis() < end) { "not found: $what" }
            SystemClock.sleep(POLL_MS)
        }
    }

    /** Nodes of every app window, topmost first: menus and dialogs are separate windows. */
    fun visible(): List<AccessibilityNodeInfo> {
        // Windows can be cached from before a new screen opened; the focused window is always read fresh.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) automation.clearCache()
        val active = automation.rootInActiveWindow
        val windows = automation.windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }.sortedByDescending { it.layer }
        val roots = windows.mapNotNull { if (it.id == active?.windowId) active else it.root }.toMutableList()
        if (active != null && roots.none { it.windowId == active.windowId }) roots.add(0, active)
        val all = ArrayList<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>(roots)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isVisibleToUser) all += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        return all
    }

    /** Nodes of the focused window only: a dialog's, not those of the screen under it. */
    fun front(): List<AccessibilityNodeInfo> {
        val active = automation.rootInActiveWindow ?: return emptyList()
        return visible().filter { it.windowId == active.windowId }
    }

    fun label(node: AccessibilityNodeInfo): String =
        listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(", ")

    /** What a node says about its state, e.g. a composer tile's «Вложение 2 из 4». */
    fun state(node: AccessibilityNodeInfo): String = node.stateDescription?.toString().orEmpty()

    /** The pieces of a node's text and description, each one a label [find] can match. */
    fun parts(node: AccessibilityNodeInfo): List<String> = label(node).split(", ", "\n").map(String::trim)
}

internal const val SHOTS_DIR = "/sdcard/Download/pawchan-readme"
internal const val LOAD_MS = 4_000L

// Shared storage ignores letter case: this folder must not be SHOTS_DIR under another spelling.
internal const val PICTURES_FOLDER = "Pawchan-attach"
private const val BACK_TRIES = 3
private const val LONG_PRESS_MS = 900
private const val LOG_NAME = "readme-log.txt"
private const val CLIP_LABEL = "Pawchan"
private const val TAG = "PawchanReadme"
private const val SETTLE_MS = 900L
private const val SHOT_SETTLE_MS = 700L
private const val RECORD_WARMUP_MS = 800L
private const val RECORD_TAIL_MS = 400L
private const val FIND_TIMEOUT_MS = 10_000L
private const val SHORT_TIMEOUT_MS = 1_000L
private const val POLL_MS = 250L
private const val MILLIS_PER_SECOND = 1_000.0
private const val SWIPE_MS = 250
private const val SCROLL_MS = 400
private const val SCROLL_TRIES = 8
private const val DEFAULT_WIDTH = 1080
private const val DEFAULT_HEIGHT = 2400

/** The type MediaStore stores with a file, from its extension. */
internal fun mimeType(name: String): String =
    when (name.substringAfterLast('.').lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        else -> "image/jpeg"
    }

internal fun isVideo(name: String): Boolean = mimeType(name).startsWith("video/")
