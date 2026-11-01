package com.solthariel.pawchan

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File

/** Local media fixture: no external network or change to the tested viewer lifecycle. */
internal fun smokeCatalogImageUrl(context: Context): String {
    val image = File(context.cacheDir, "catalog-return-fixture.png")
    val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    try {
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    } finally {
        bitmap.recycle()
    }
    return Uri.fromFile(image).toString()
}
