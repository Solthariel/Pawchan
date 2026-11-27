package com.solthariel.pawchan.media.video

/** Main-thread playback snapshot and its delivery to the current session. */
internal class MpvPlaybackEvents {
    private var callback: (VideoEvent, VideoPlaybackStatus) -> Unit = { _, _ -> }
    private var snapshot = VideoPlaybackStatus()

    fun open(onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit) {
        callback = onEvent
        snapshot = VideoPlaybackStatus()
    }

    fun emit(event: VideoEvent) = callback(event, snapshot)

    fun sample(
        sample: MpvPollSample,
        waitingForBytes: Boolean = false,
    ) {
        if (snapshot.ended && !sample.hasFile) return
        val stalled = waitingForBytes && !sample.paused && sample.position == snapshot.position
        snapshot =
            snapshot.copy(
                stage = pollPlaybackStage(snapshot.ended, sample.hasFile, sample.buffering || stalled),
                timeline = VideoTimeline(sample.position, sample.duration),
                playing = sample.hasFile && !sample.paused && !snapshot.ended,
                seekable = sample.duration > 0,
                output = VideoOutputDetails(sample.hardwareDecoded, sample.aspect),
            )
        emit(VideoEvent.PROGRESS)
    }

    fun sourceError() {
        callback(VideoEvent.ERROR, snapshot.copy(attempt = snapshot.attempt.copy(failure = VideoFailure.SOURCE)))
    }

    fun end(reason: String?) {
        when (reason) {
            "eof" -> {
                snapshot = snapshot.copy(ended = true, playing = false)
                emit(VideoEvent.ENDED)
            }

            "error" -> {
                emit(VideoEvent.ERROR)
            }
        }
    }

    fun resumeOrSeek() {
        snapshot = snapshot.copy(ended = false)
    }

    fun close() {
        callback = { _, _ -> }
    }
}
