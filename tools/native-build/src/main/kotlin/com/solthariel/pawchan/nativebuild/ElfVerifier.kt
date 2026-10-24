package com.solthariel.pawchan.nativebuild

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path

private data class ProgramTable(
    val wide: Boolean,
    val offset: Long,
    val stride: Int,
    val count: Int,
)

internal object ElfVerifier {
    fun verify(
        file: Path,
        abi: AndroidAbi,
    ) {
        FileChannel.open(file).use { channel ->
            val table = header(read(channel, 0, 64), abi)
            check(
                table.count in 1..1024 && table.offset >= (if (table.wide) 64 else 52) &&
                    table.offset + table.stride.toLong() * table.count <= channel.size(),
            ) {
                "Invalid ELF program table: $file"
            }
            var loads = 0
            repeat(table.count) { index ->
                val segment = read(channel, table.offset + table.stride.toLong() * index, table.stride)
                if (segment.getInt(0) == 1) {
                    verifyLoad(segment, table.wide)
                    loads++
                }
            }
            check(loads > 0) { "No ELF load segments: $file" }
        }
    }

    private fun header(
        header: ByteBuffer,
        abi: AndroidAbi,
    ): ProgramTable {
        check(header.getInt(0) == 0x464c457f && header.get(5).toInt() == 1) { "Not a little-endian ELF" }
        val wide = header.get(4).toInt() == 2
        check(header.get(4).toInt() in 1..2 && header.getShort(16).toInt() == 3) { "Not an ELF shared object" }
        val machine =
            when (abi) {
                AndroidAbi.ARM64 -> 183
                AndroidAbi.ARM32 -> 40
                AndroidAbi.X86 -> 3
                AndroidAbi.X64 -> 62
            }
        check(header.getShort(18).toInt() == machine) { "Wrong ELF machine for ${abi.id}" }
        check(wide == (abi == AndroidAbi.ARM64 || abi == AndroidAbi.X64)) { "Wrong ELF class" }
        val offset = if (wide) header.getLong(32) else header.getInt(28).toLong() and 0xffffffffL
        val stride = header.getShort(if (wide) 54 else 42).toInt() and 0xffff
        val count = header.getShort(if (wide) 56 else 44).toInt() and 0xffff
        check(stride >= (if (wide) 56 else 32))
        return ProgramTable(wide, offset, stride, count)
    }

    private fun verifyLoad(
        segment: ByteBuffer,
        wide: Boolean,
    ) {
        val alignment = if (wide) segment.getLong(48) else segment.getInt(28).toLong() and 0xffffffffL
        check(alignment >= 16384 && alignment and (alignment - 1) == 0L) { "ELF is not 16 KiB-page aligned" }
        val offset = if (wide) segment.getLong(8) else segment.getInt(4).toLong() and 0xffffffffL
        val address = if (wide) segment.getLong(16) else segment.getInt(8).toLong() and 0xffffffffL
        check(offset >= 0 && address >= 0 && offset % alignment == address % alignment) { "ELF load mapping is misaligned" }
    }

    private fun read(
        channel: FileChannel,
        offset: Long,
        size: Int,
    ): ByteBuffer {
        val bytes = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        while (bytes.hasRemaining()) check(channel.read(bytes, offset + bytes.position()) > 0) { "Truncated ELF" }
        return bytes
    }
}
