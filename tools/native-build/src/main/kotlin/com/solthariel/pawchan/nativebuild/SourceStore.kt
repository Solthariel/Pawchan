package com.solthariel.pawchan.nativebuild

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

internal class SourceStore(
    private val root: Path,
    private val cache: ArchiveCache,
    private val runner: CommandRunner,
) {
    fun prepare(
        spec: DependencySpec,
        provided: Path?,
        offline: Boolean,
    ): Path {
        Files.createDirectories(root)
        val target = root.resolve(spec.name)
        if (provided != null) {
            require(Files.isDirectory(provided)) { "Missing supplied source: $provided" }
            FileIntegrity.requireSeparate(provided, root)
        }
        val identity =
            if (provided ==
                null
            ) {
                FileIntegrity.text(spec.toString())
            } else {
                "local:${provided.toRealPath()}:${FileIntegrity.tree(provided)}"
            }
        val record = root.resolve("${spec.name}.properties")
        if (isCurrent(target, record, identity)) return target
        val stage = Files.createTempDirectory(root, "${spec.name}-")
        try {
            if (provided != null) FileIntegrity.copyTree(provided, stage) else acquire(spec, stage, offline)
            val required = if (spec.name == "ffmpeg") "configure" else "meson.build"
            require(Files.isRegularFile(stage.resolve(required))) { "Missing $required in ${spec.name} source" }
            val hash = FileIntegrity.tree(stage)
            FileIntegrity.replace(stage, target)
            val properties =
                Properties().apply {
                    setProperty("identity", identity)
                    setProperty("tree", hash)
                }
            Files.newOutputStream(record).use { properties.store(it, "Pawchan prepared source") }
            return target
        } finally {
            stage.toFile().deleteRecursively()
        }
    }

    private fun isCurrent(
        target: Path,
        record: Path,
        identity: String,
    ): Boolean {
        if (!Files.isDirectory(target) || !Files.isRegularFile(record)) return false
        val properties = Properties().apply { Files.newInputStream(record).use { load(it) } }
        return properties.getProperty("identity") == identity && properties.getProperty("tree") == FileIntegrity.tree(target)
    }

    private fun acquire(
        spec: DependencySpec,
        stage: Path,
        offline: Boolean,
    ) {
        val archive =
            try {
                cache.obtain(spec, offline)
            } catch (failure: SourceUnavailable) {
                if (spec.git == null || offline) throw failure
                checkout(spec.git, stage)
                return
            }
        SourceArchive.extract(archive, stage)
    }

    private fun checkout(
        git: GitSource,
        stage: Path,
    ) {
        runner.run(Command(listOf("git", "init", "--quiet", "."), stage))
        runner.run(Command(listOf("git", "remote", "add", "origin", git.url), stage))
        runner.run(Command(listOf("git", "fetch", "--quiet", "--depth=1", "origin", git.ref), stage))
        val actual = runner.run(Command(listOf("git", "rev-parse", "FETCH_HEAD^{commit}"), stage, capture = true))
        if (actual != git.commit) throw IntegrityFailure("Pinned Git commit mismatch: expected ${git.commit}, received $actual")
        runner.run(Command(listOf("git", "checkout", "--quiet", "--detach", "FETCH_HEAD"), stage))
        check(stage.resolve(".git").toFile().deleteRecursively())
    }
}
