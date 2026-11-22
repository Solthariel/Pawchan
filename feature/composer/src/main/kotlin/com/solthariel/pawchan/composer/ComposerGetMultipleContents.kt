package com.solthariel.pawchan.composer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/** The same GET_CONTENT entry point as Dashchan, retaining Pawchan's supported types. */
internal class ComposerGetMultipleContents : ActivityResultContract<Array<String>, List<Uri>>() {
    override fun createIntent(
        context: Context,
        input: Array<String>,
    ): Intent =
        Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, input)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): List<Uri> = ActivityResultContracts.OpenMultipleDocuments().parseResult(resultCode, intent)
}
