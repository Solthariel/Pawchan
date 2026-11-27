package com.solthariel.pawchan

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * Explains a threadshot pixel mismatch: where the pixels are, which colors turned into which, and which channel moved.
 * Saves the reference, the threadshot and a marked diff to /sdcard/Download/pawchan-shot-diff/<name>.
 */
internal class ThreadShotDiff(
    private val expected: IntArray,
    private val actual: IntArray,
    private val width: Int,
) {
    private val indices = expected.indices.filter { it < actual.size && expected[it] != actual[it] }

    val isEmpty: Boolean get() = indices.isEmpty()

    fun describe(): String {
        if (indices.isEmpty()) return ""
        val xs = indices.map { it % width }
        val ys = indices.map { it / width }
        val colors =
            indices
                .groupingBy { hex(expected[it]) + "->" + hex(actual[it]) }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(TOP)
                .joinToString { "${it.key} x${it.value}" }
        val cells =
            indices
                .groupingBy { "${it % width / CELL * CELL},${it / width / CELL * CELL}" }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(TOP)
                .joinToString { "${it.key} x${it.value}" }
        return "${indices.size} pixels differ, box (${xs.min()},${ys.min()})-(${xs.max()},${ys.max()}); " +
            "channels ${channelMoves()}; colors $colors; ${CELL}px cells $cells"
    }

    fun save(
        name: String,
        reference: Bitmap,
        shot: Bitmap,
    ): String {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val root = checkNotNull(instrumentation.targetContext.getExternalFilesDir(null))
        val dir = File(root, "shot-diff/$name").apply { mkdirs() }
        write(File(dir, "reference.png"), reference)
        write(File(dir, "threadshot.png"), shot)
        write(File(dir, "diff.png"), marked(reference))
        shell("mkdir -p $SHARED")
        shell("cp -r ${dir.absolutePath} $SHARED/")
        return "$SHARED/$name"
    }

    private fun channelMoves(): Map<String, Int> =
        indices
            .flatMap { i ->
                CHANNELS.mapNotNull { (channel, shift) ->
                    val delta = (actual[i] ushr shift and 0xFF) - (expected[i] ushr shift and 0xFF)
                    if (delta == 0) null else channel + (if (delta > 0) "+" else "") + delta
                }
            }.groupingBy { it }
            .eachCount()

    private fun marked(reference: Bitmap): Bitmap {
        val pixels = expected.map { dim(it) }.toIntArray()
        indices.forEach { pixels[it] = MARK }
        return Bitmap.createBitmap(pixels, width, reference.height, Bitmap.Config.ARGB_8888)
    }

    private fun write(
        file: File,
        image: Bitmap,
    ) {
        val software = if (image.config == Bitmap.Config.HARDWARE) image.copy(Bitmap.Config.ARGB_8888, false) else image
        file.outputStream().use { software.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
    }

    private fun shell(command: String) {
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }
}

/** Formats of both images: a different config or color space would explain one-step rounding differences. */
internal fun shotFormats(
    reference: Bitmap,
    shot: Bitmap,
): String = "reference ${reference.config}/${reference.colorSpace?.name}, threadshot ${shot.config}/${shot.colorSpace?.name}"

private fun hex(color: Int): String = "#%08X".format(color)

/** Keeps the picture readable under the magenta marks. */
private fun dim(color: Int): Int {
    val r = (color ushr 16 and 0xFF) / 2 + HALF
    val g = (color ushr 8 and 0xFF) / 2 + HALF
    val b = (color and 0xFF) / 2 + HALF
    return OPAQUE or (r shl 16) or (g shl 8) or b
}

private val CHANNELS = listOf("A" to 24, "R" to 16, "G" to 8, "B" to 0)
private const val SHARED = "/sdcard/Download/pawchan-shot-diff"
private const val CELL = 16
private const val TOP = 8
private const val PNG_QUALITY = 100
private const val HALF = 128
private const val OPAQUE = 0xFF shl 24
private val MARK = 0xFFFF00FF.toInt()
