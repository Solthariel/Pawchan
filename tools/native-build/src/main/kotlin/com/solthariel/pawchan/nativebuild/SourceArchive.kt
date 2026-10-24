package com.solthariel.pawchan.nativebuild

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.nio.file.Files
import java.nio.file.Path

internal object SourceArchive {
    fun extract(
        archive: Path,
        target: Path,
    ) {
        Files.createDirectories(target)
        val extraction = ArchiveExtraction(target)
        Files.newInputStream(archive).buffered().use { input ->
            CompressorStreamFactory().createCompressorInputStream(input).use { compressed ->
                TarArchiveInputStream(compressed).use { extraction.read(it) }
            }
        }
        require(FileIntegrity.files(target).isNotEmpty()) { "Empty source archive" }
    }
}

private class ArchiveExtraction(
    private val target: Path,
) {
    private var rootName: String? = null
    private var expanded = 0L
    private var entries = 0
    private val seen = mutableSetOf<Path>()

    fun read(tar: TarArchiveInputStream) {
        var entry = tar.nextEntry
        while (entry != null) {
            require(++entries <= 200_000) { "Too many archive entries" }
            val path = validate(entry)
            if (path.nameCount > 1) copy(entry, path, tar) else require(entry.isDirectory) { "Missing source root directory" }
            entry = tar.nextEntry
        }
    }

    private fun validate(entry: TarArchiveEntry): Path {
        val path = Path.of(entry.name)
        require(
            !path.isAbsolute && !entry.name.startsWith("/") && path.none { it.toString() == ".." } &&
                '\\' !in entry.name && ':' !in entry.name,
        ) { "Unsafe archive path: ${entry.name}" }
        require(path.nameCount > 0 && path.getName(0).toString() !in setOf(".", ""))
        val top = path.getName(0).toString()
        if (rootName == null) rootName = top
        require(rootName == top) { "Expected one source root" }
        require(
            entry.isDirectory || (entry.isFile && !entry.isLink && !entry.isSymbolicLink),
        ) { "Archive links/special files are not accepted" }
        return path
    }

    private fun copy(
        entry: TarArchiveEntry,
        path: Path,
        tar: TarArchiveInputStream,
    ) {
        val output = target.resolve(path.subpath(1, path.nameCount)).normalize()
        require(output.startsWith(target.normalize()) && seen.add(output)) { "Duplicate or escaping archive entry" }
        if (entry.isDirectory) {
            Files.createDirectories(output)
            return
        }
        expanded += entry.size
        require(entry.size >= 0 && expanded <= 2L * 1024 * 1024 * 1024) { "Source archive exceeds extraction budget" }
        Files.createDirectories(output.parent)
        Files.newOutputStream(output).use { tar.copyTo(it) }
        if (entry.mode and 73 != 0) check(output.toFile().setExecutable(true, false))
    }
}
