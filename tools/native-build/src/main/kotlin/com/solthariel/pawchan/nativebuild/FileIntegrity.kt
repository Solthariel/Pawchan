package com.solthariel.pawchan.nativebuild

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

internal object FileIntegrity {
    fun digest(path: Path): String =
        Files.newInputStream(path).use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            var count = input.read(bytes)
            while (count >= 0) {
                digest.update(bytes, 0, count)
                count = input.read(bytes)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }

    fun text(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") {
            "%02x".format(it)
        }

    fun files(root: Path): List<Path> =
        Files.walk(root).use { paths ->
            paths
                .filter { it != root && it.none { part -> part.toString() == ".git" } }
                .sorted()
                .toList()
                .also { entries ->
                    require(entries.none { Files.isSymbolicLink(it) }) { "Source trees must not contain symbolic links: $root" }
                    require(
                        entries.all { Files.isDirectory(it) || Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) },
                    ) { "Special file in $root" }
                }.filter { Files.isRegularFile(it) }
        }

    fun tree(root: Path): String = text(files(root).joinToString("\n") { "${root.relativize(it)}:${digest(it)}:${Files.isExecutable(it)}" })

    fun copyTree(
        source: Path,
        target: Path,
    ) {
        files(source).forEach { file ->
            val output = target.resolve(source.relativize(file).toString())
            Files.createDirectories(output.parent)
            Files.copy(file, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
        }
    }

    fun requireSeparate(
        first: Path,
        second: Path,
    ) {
        val left = first.toFile().canonicalFile.toPath()
        val right = second.toFile().canonicalFile.toPath()
        require(!left.startsWith(right) && !right.startsWith(left)) { "Overlapping source and destination: $left, $right" }
    }

    fun replace(
        staged: Path,
        target: Path,
    ) {
        Files.createDirectories(target.parent)
        val backup = target.resolveSibling("${target.fileName}.previous")
        check(!Files.exists(backup)) { "Unresolved previous publication at $backup; inspect it before retrying" }
        val hadTarget = Files.exists(target)
        if (hadTarget) Files.move(target, backup, StandardCopyOption.ATOMIC_MOVE)
        try {
            Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (failure: Exception) {
            if (hadTarget) Files.move(backup, target, StandardCopyOption.ATOMIC_MOVE)
            throw failure
        }
        backup.toFile().deleteRecursively()
    }
}
