package com.solthariel.pawchan.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Each attempt owns its partial file; only a verified file can replace the install candidate. */
internal class UpdateDownloadStore(
    private val directory: File,
    private val verify: (File, UpdatePackage) -> Unit,
) {
    suspend fun download(
        release: UpdatePackage,
        transfer: suspend (File) -> Unit,
    ): File =
        withContext(Dispatchers.IO) {
            check(release.length in 1..MAX_APK_BYTES) { "Invalid update length" }
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create update directory" }
            val partial = File.createTempFile("Pawchan-update-", ".part", directory)
            try {
                transfer(partial)
                currentCoroutineContext().ensureActive()
                verify(partial, release)
                currentCoroutineContext().ensureActive()
                val target = File(directory, "Pawchan-update.apk")
                Files.move(
                    partial.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
                target
            } finally {
                partial.delete()
            }
        }
}
