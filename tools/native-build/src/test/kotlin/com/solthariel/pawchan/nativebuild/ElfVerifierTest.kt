package com.solthariel.pawchan.nativebuild

import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class ElfVerifierTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun allSupportedElfClassesAndMachinesPass() {
        AndroidAbi.entries.forEach { abi ->
            val file = temp.root.toPath().resolve(abi.id)
            Fixtures.elf(file, abi)
            ElfVerifier.verify(file, abi)
        }
    }

    @Test fun differentMachineIsRejected() {
        val file = temp.root.toPath().resolve("library")
        Fixtures.elf(file, AndroidAbi.X64)
        assertThrows(IllegalStateException::class.java) { ElfVerifier.verify(file, AndroidAbi.ARM64) }
    }

    @Test fun fourKiBAlignmentIsRejected() {
        val file = temp.root.toPath().resolve("library")
        Fixtures.elf(file, AndroidAbi.ARM64, 4096)
        assertThrows(IllegalStateException::class.java) { ElfVerifier.verify(file, AndroidAbi.ARM64) }
    }

    @Test fun nonPowerOfTwoAlignmentIsRejected() {
        val file = temp.root.toPath().resolve("library")
        Fixtures.elf(file, AndroidAbi.ARM64, 20000)
        assertThrows(IllegalStateException::class.java) { ElfVerifier.verify(file, AndroidAbi.ARM64) }
    }

    @Test fun truncatedOrNonElfDataIsRejected() {
        val file = Fixtures.write(temp.root.toPath().resolve("library"), "not an ELF")
        assertThrows(IllegalStateException::class.java) { ElfVerifier.verify(file, AndroidAbi.ARM64) }
        Files.write(file, ByteArray(128))
        assertThrows(IllegalStateException::class.java) { ElfVerifier.verify(file, AndroidAbi.ARM64) }
    }
}
