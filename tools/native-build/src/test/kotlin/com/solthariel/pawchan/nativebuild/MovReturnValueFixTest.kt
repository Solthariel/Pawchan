package com.solthariel.pawchan.nativebuild

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class MovReturnValueFixTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun knownCallIsFixedAndOtherTextIsPreserved() {
        val file = Fixtures.write(temp.root.toPath().resolve("libavformat/mov.c"), "before\n\tff_mov_read_chnl(c->fc, pb, st);\nafter\n")
        MovReturnValueFix.apply(temp.root.toPath())
        assertEquals("before\n\tret = ff_mov_read_chnl(c->fc, pb, st);\nafter\n", Files.readString(file))
    }

    @Test fun alreadyCorrectedSourceIsUnchanged() {
        val text = "    ret = ff_mov_read_chnl(c->fc, pb, st);\n"
        val file = Fixtures.write(temp.root.toPath().resolve("libavformat/mov.c"), text)
        MovReturnValueFix.apply(temp.root.toPath())
        assertEquals(text, Files.readString(file))
    }

    @Test fun unknownSourceFailsInsteadOfSilentlyClaimingSuccess() {
        Fixtures.write(temp.root.toPath().resolve("libavformat/mov.c"), "upstream changed")
        assertThrows(IllegalStateException::class.java) { MovReturnValueFix.apply(temp.root.toPath()) }
    }

    @Test fun ambiguousCallsAreNotChanged() {
        val text = "    ff_mov_read_chnl(c->fc, pb, st);\n".repeat(2)
        val file = Fixtures.write(temp.root.toPath().resolve("libavformat/mov.c"), text)
        assertThrows(IllegalStateException::class.java) { MovReturnValueFix.apply(temp.root.toPath()) }
        assertEquals(text, Files.readString(file))
    }
}
