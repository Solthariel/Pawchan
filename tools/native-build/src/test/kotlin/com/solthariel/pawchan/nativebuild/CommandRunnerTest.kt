package com.solthariel.pawchan.nativebuild

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean

class CommandRunnerTest {
    @get:Rule val temp = TemporaryFolder()

    private fun probe(vararg arguments: String): Command {
        val executable = if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"
        val java = File(System.getProperty("java.home"), "bin/$executable").absolutePath
        val classpath =
            listOf(ProcessProbe::class.java, Unit::class.java).joinToString(File.pathSeparator) {
                File(
                    it.protectionDomain.codeSource.location
                        .toURI(),
                ).absolutePath
            }
        return Command(listOf(java, "-cp", classpath, ProcessProbe::class.java.name) + arguments, temp.root.toPath(), capture = true)
    }

    @Test fun argumentsWithSpacesAndMetacharactersAreNotInterpretedByShell() {
        val value = "path with spaces; not a command"
        ProcessCommandRunner(temp.root.toPath().resolve("logs")).use { runner -> assertEquals(value, runner.run(probe("echo", value))) }
    }

    @Test fun nonZeroExitProducesFailureAndKeepsLog() {
        ProcessCommandRunner(temp.root.toPath().resolve("logs")).use { runner ->
            assertThrows(IllegalStateException::class.java) { runner.run(probe("fail")) }
        }
        val log =
            temp.root
                .toPath()
                .resolve("logs")
                .toFile()
                .listFiles()!!
                .single()
        assertEquals("failure", Files.readString(log.toPath()))
    }

    @Test fun timeoutTerminatesLongRunningProcess() {
        ProcessCommandRunner(temp.root.toPath().resolve("logs"), 1).use { runner ->
            assertThrows(IllegalStateException::class.java) { runner.run(probe("sleep")) }
        }
    }

    @Test fun interruptionCancelsWaitAndPreservesInterruptedStatus() {
        val cancelled = AtomicBoolean()
        ProcessCommandRunner(temp.root.toPath().resolve("logs")).use { runner ->
            val thread =
                Thread {
                    try {
                        runner.run(probe("sleep"))
                    } catch (_: InterruptedException) {
                        cancelled.set(Thread.currentThread().isInterrupted)
                    }
                }
            thread.start()
            Thread.sleep(250)
            thread.interrupt()
            thread.join(5000)
            assertFalse("Interrupted process owner did not finish", thread.isAlive)
            assertTrue(cancelled.get())
        }
    }
}
