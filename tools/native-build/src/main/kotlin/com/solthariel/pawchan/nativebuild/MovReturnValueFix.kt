package com.solthariel.pawchan.nativebuild

import java.nio.file.Files
import java.nio.file.Path

/** Retains the known MOV correction from the prior Slooop-derived build, not a claim of new authorship. */
internal object MovReturnValueFix {
    fun apply(source: Path) {
        val file = source.resolve("libavformat/mov.c")
        val original = Files.readString(file)
        val unchecked = Regex("(?m)^([ \\t]*)ff_mov_read_chnl\\(c->fc, pb, st\\);[ \\t]*$")
        val checked = Regex("(?m)^[ \\t]*ret = ff_mov_read_chnl\\(c->fc, pb, st\\);[ \\t]*$")
        val matches = unchecked.findAll(original).toList()
        if (matches.isEmpty()) {
            check(checked.findAll(original).count() == 1) { "Unrecognized pinned MOV source: no single known call" }
            return
        }
        check(matches.size == 1 && !checked.containsMatchIn(original)) { "Ambiguous MOV correction" }
        Files.writeString(file, unchecked.replace(original) { "${it.groupValues[1]}ret = ff_mov_read_chnl(c->fc, pb, st);" })
    }
}
