package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

internal fun saveThreadChromeScreenshot(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "thread-chrome-check")
    check(folder.mkdirs() || folder.isDirectory)
    val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
    File(folder, "$name.png").outputStream().use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
    }
    bitmap.recycle()
}
