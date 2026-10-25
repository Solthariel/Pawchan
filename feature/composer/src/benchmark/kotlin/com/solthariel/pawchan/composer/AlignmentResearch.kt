package com.solthariel.pawchan.composer

import android.os.Process
import android.os.SystemClock
import android.util.Log
import org.json.JSONObject

/** UI-thread-only counters for the benchmark viewport, not all Compose nodes. */
internal object AlignmentResearch {
    private var active = false
    private var action = 0
    private var measures = 0
    private var placements = 0
    private var measureNs = 0L

    fun toggle(expanding: Boolean) {
        if (expanding) {
            check(!active) { "Research action already open" }
            active = true
            action++
            measures = 0
            placements = 0
            measureNs = 0L
        } else {
            check(active) { "Research action missing" }
            active = false
            val row =
                JSONObject()
                    .put("schema", 1)
                    .put("pid", Process.myPid())
                    .put("action", action)
                    .put("measureCalls", measures)
                    .put("placeCalls", placements)
                    .put("measureNs", measureNs)
            Log.i("PawResearch", "PAW_RESEARCH $row")
        }
    }

    fun <T> measure(block: () -> T): T {
        if (!active) return block()
        val start = SystemClock.elapsedRealtimeNanos()
        measures++
        return try {
            block()
        } finally {
            measureNs += SystemClock.elapsedRealtimeNanos() - start
        }
    }

    fun placed() {
        if (active) placements++
    }
}
