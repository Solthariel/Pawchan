package com.solthariel.pawchan.composer

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerGetMultipleContentsTest {
    @Test fun intentRequestsOpenableMultipleMedia() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val types = arrayOf("image/*", "video/*")
        val intent = ComposerGetMultipleContents().createIntent(context, types)
        assertEquals(Intent.ACTION_GET_CONTENT, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertEquals("*/*", intent.type)
        assertArrayEquals(types, intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES))
        assertTrue(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
    }

    @Test fun multipleUrisAreReturnedAndCancellationIsEmpty() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = Uri.parse("content://media/first")
        val second = Uri.parse("content://media/second")
        val clip = ClipData.newUri(context.contentResolver, "media", first).apply { addItem(ClipData.Item(second)) }
        val result = Intent().apply { clipData = clip }
        val contract = ComposerGetMultipleContents()
        assertEquals(listOf(first, second), contract.parseResult(Activity.RESULT_OK, result))
        assertTrue(contract.parseResult(Activity.RESULT_CANCELED, null).isEmpty())
    }
}
