package com.solthariel.pawchan.nativebuild

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal data class Command(
    val arguments: List<String>,
    val directory: Path,
    val environment: Map<String, String> = emptyMap(),
    val capture: Boolean = false,
)

internal fun interface CommandRunner {
    fun run(command: Command): String
}

internal class ProcessCommandRunner(
    private val logRoot: Path,
    private val timeoutSeconds: Long = 7200,
) : CommandRunner,
    AutoCloseable {
    private val active = AtomicReference<Process?>()
    private val shutdown = Thread({ terminate() }, "pawchan-native-process-cleanup")

    init {
        Runtime.getRuntime().addShutdownHook(shutdown)
    }

    @Synchronized
    override fun run(command: Command): String {
        Files.createDirectories(logRoot)
        val log = Files.createTempFile(logRoot, "process-", ".log")
        val builder =
            ProcessBuilder(
                command.arguments,
            ).directory(command.directory.toFile()).redirectErrorStream(true).redirectOutput(log.toFile())
        builder.environment().putAll(command.environment)
        val process = builder.start()
        check(active.compareAndSet(null, process)) { "A process is already running" }
        println("Executing ${command.arguments.first()}; log: $log")
        try {
            check(process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) { "Process timed out; see $log" }
            check(process.exitValue() == 0) { "Process exit ${process.exitValue()}: ${command.arguments.first()}; see $log" }
            if (!command.capture) return ""
            check(Files.size(log) <= 1024 * 1024) { "Captured process output exceeds 1 MiB; see $log" }
            return Files.readString(log).trim()
        } catch (cancelled: InterruptedException) {
            Thread.currentThread().interrupt()
            throw cancelled
        } finally {
            terminate()
            active.set(null)
        }
    }

    private fun terminate() {
        active.get()?.let { process ->
            process.toHandle().descendants().use { children -> children.forEach { it.destroyForcibly() } }
            if (process.isAlive) process.destroyForcibly()
        }
    }

    override fun close() {
        terminate()
        Runtime.getRuntime().removeShutdownHook(shutdown)
    }
}
