package com.solthariel.pawchan.update

import java.io.File

/** Operations owned by the update ViewModel; implementations own and close their transport. */
internal interface UpdateOperations : AutoCloseable {
    suspend fun check(): Pair<UpdatePackage, String?>?

    suspend fun download(
        release: UpdatePackage,
        onProgress: (Int) -> Unit,
    ): File
}
