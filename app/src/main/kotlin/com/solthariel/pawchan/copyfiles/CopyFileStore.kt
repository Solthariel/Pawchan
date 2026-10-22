package com.solthariel.pawchan.copyfiles

import java.io.OutputStream

/** A document-tree capability; callers never parse provider paths or perform resolver I/O. */
internal interface CopyFileStore {
    suspend fun write(
        tree: String,
        kind: CopyFileKind,
        name: String,
        writer: suspend (OutputStream) -> Unit,
    ): CopyDocument

    suspend fun list(
        tree: String,
        kind: CopyFileKind,
    ): List<CopyDocument>
}
