package com.solthariel.pawchan.ffmpeg

/** A reader only sees byte ranges that the source has finished writing. */
interface ProgressiveInputSink {
    fun publish(
        prefix: Long,
        total: Long,
    )

    fun publishTail(
        start: Long,
        end: Long,
    )
}

/** Reader callbacks for progressive data and missing ranges. */
interface ProgressiveReadCallback {
    fun onReadReady()

    fun requestPartFromPosition(position: Long)
}
