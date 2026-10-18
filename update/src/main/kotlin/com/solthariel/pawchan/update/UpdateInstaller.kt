package com.solthariel.pawchan.update

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

internal object UpdateInstaller {
    fun launch(
        context: Context,
        apk: File,
    ): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val permissionIntent =
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${context.packageName}".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(permissionIntent)
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.update-files", apk)
        val installIntent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(installIntent)
        return true
    }

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
}
