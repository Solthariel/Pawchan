package com.solthariel.pawchan.backup

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Our format emits a small, single-disk ZIP without ZIP64 or archive comments. */
internal fun validateZipDirectory(bytes: ByteArray): Int {
    require(bytes.size >= ZIP_END_BYTES)
    val endOffset = bytes.size - ZIP_END_BYTES
    val end = ByteBuffer.wrap(bytes, endOffset, ZIP_END_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    require(end.int == ZIP_END_SIGNATURE) { "Обрезанный или повреждённый архив" }
    require(end.short.toInt() == 0 && end.short.toInt() == 0)
    val diskEntries = end.short.toInt() and UNSIGNED_SHORT_MASK
    val allEntries = end.short.toInt() and UNSIGNED_SHORT_MASK
    require(diskEntries == allEntries && allEntries in 1..(BackupSection.entries.size + 1))
    val directoryBytes = end.int.toLong() and UNSIGNED_INT_MASK
    val directoryOffset = end.int.toLong() and UNSIGNED_INT_MASK
    require(end.short.toInt() == 0)
    require(directoryOffset >= MIN_ZIP_HEADER_BYTES && directoryOffset + directoryBytes == endOffset.toLong())
    return allEntries
}

private const val ZIP_END_BYTES = 22
private const val ZIP_END_SIGNATURE = 0x06054b50
private const val UNSIGNED_SHORT_MASK = 0xffff
private const val UNSIGNED_INT_MASK = 0xffffffffL
private const val MIN_ZIP_HEADER_BYTES = 30L
