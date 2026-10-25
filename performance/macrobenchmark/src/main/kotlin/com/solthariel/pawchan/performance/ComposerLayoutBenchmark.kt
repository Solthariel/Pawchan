package com.solthariel.pawchan.performance

import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Warm in-process layout transitions, not app startup or a full-app scrolling benchmark. */
@RunWith(AndroidJUnit4::class)
class ComposerLayoutBenchmark {
    @get:Rule
    val benchmark = MacrobenchmarkRule()

    @Test
    fun warmErrorLayout() {
        val diagnostics = InstrumentationRegistry.getArguments().getString("pawDiagnostics") == "true"
        runCatching { runScenario(diagnostics) }
            .onFailure { failure ->
                if (diagnostics) {
                    saveDiagnosticFailure(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()), failure)
                }
            }.getOrThrow()
    }

    private fun runScenario(diagnostics: Boolean) {
        benchmark.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Full(),
            iterations = MEASURED_ITERATIONS,
            setupBlock = {
                killProcess()
                startActivityAndWait { intent ->
                    intent.putExtra("paw_diag", diagnostics)
                    intent.putExtra(
                        "paw_research",
                        InstrumentationRegistry.getArguments().getString("pawResearch") == "true",
                    )
                }
                awaitState(device, "fixture_short")
                exercise(device, WARMUP_CYCLES)
            },
        ) {
            exercise(device, MEASURED_CYCLES)
        }
    }
}

private fun exercise(
    device: UiDevice,
    cycles: Int,
) {
    repeat(cycles) {
        requireNotNull(device.wait(Until.findObject(By.res("toggle_error")), UI_TIMEOUT_MS)).click()
        awaitState(device, "fixture_long")
        val viewport = requireNotNull(device.wait(Until.findObject(By.res("status_viewport")), UI_TIMEOUT_MS))
        viewport.setGestureMargin(GESTURE_MARGIN_PX)
        viewport.swipe(Direction.UP, GESTURE_PERCENT)
        device.waitForIdle()
        viewport.swipe(Direction.DOWN, GESTURE_PERCENT)
        device.waitForIdle()
        requireNotNull(device.findObject(By.res("toggle_error"))).click()
        awaitState(device, "fixture_short")
    }
}

private fun awaitState(
    device: UiDevice,
    text: String,
) {
    check(device.wait(Until.hasObject(By.res("fixture_state").text(text)), UI_TIMEOUT_MS)) {
        "Fixture did not reach $text; the run is invalid."
    }
    device.waitForIdle()
}

private const val TARGET_PACKAGE = "com.solthariel.pawchan.performance"
private const val MEASURED_ITERATIONS = 5
private const val WARMUP_CYCLES = 2
private const val MEASURED_CYCLES = 10
private const val UI_TIMEOUT_MS = 10_000L
private const val GESTURE_MARGIN_PX = 16
private const val GESTURE_PERCENT = 0.8f

/** Runs only after a failed diagnostic test; never retries or masks the original failure. */
private fun saveDiagnosticFailure(
    device: UiDevice,
    failure: Throwable,
) {
    runCatching {
        val path = checkNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"))
        val directory = File(path)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create diagnostic output directory" }
        check(device.takeScreenshot(File(directory, "pawdiag-failure.png"))) { "Screenshot capture failed" }
    }.onFailure { Log.e("PawDiag", "Failure screenshot unavailable", it) }
    runCatching {
        val path = checkNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"))
        device.dumpWindowHierarchy(File(path, "pawdiag-failure-ui.xml"))
        File(path, "pawdiag-failure.txt").writeText(failure.stackTraceToString())
    }.onFailure { Log.e("PawDiag", "Failure hierarchy unavailable", it) }
}
