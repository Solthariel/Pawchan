package com.solthariel.pawchan.media

import coil3.EventListener
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.size.SizeResolver
import com.solthariel.pawchan.ffmpeg.MediaTiming
import com.solthariel.pawchan.ffmpeg.TimingProbe.Event

/** One probe per Coil operation. No request data, cache keys or errors enter the log. */
class MediaImageTiming private constructor() : EventListener() {
    private val timing = MediaTiming.create()

    companion object {
        fun create(): EventListener = if (MediaTiming.enabled()) MediaImageTiming() else EventListener.NONE
    }

    override fun onStart(request: ImageRequest) = timing.mark(Event.IMAGE_START, 0)

    override fun resolveSizeStart(
        request: ImageRequest,
        sizeResolver: SizeResolver,
    ) = timing.mark(Event.IMAGE_SIZE_START, 0)

    override fun resolveSizeEnd(
        request: ImageRequest,
        size: Size,
    ) = timing.mark(Event.IMAGE_SIZE_END, 0)

    override fun fetchStart(
        request: ImageRequest,
        fetcher: Fetcher,
        options: Options,
    ) = timing.mark(Event.IMAGE_FETCH_START, 0)

    override fun fetchEnd(
        request: ImageRequest,
        fetcher: Fetcher,
        options: Options,
        result: FetchResult?,
    ) = timing.mark(Event.IMAGE_FETCH_END, 0)

    override fun decodeStart(
        request: ImageRequest,
        decoder: Decoder,
        options: Options,
    ) = timing.mark(Event.IMAGE_DECODE_START, 0)

    override fun decodeEnd(
        request: ImageRequest,
        decoder: Decoder,
        options: Options,
        result: DecodeResult?,
    ) = timing.mark(Event.IMAGE_DECODE_END, 0)

    override fun onSuccess(
        request: ImageRequest,
        result: SuccessResult,
    ) = timing.mark(Event.IMAGE_SUCCESS, result.dataSource.ordinal.toLong())

    override fun onError(
        request: ImageRequest,
        result: ErrorResult,
    ) = timing.mark(Event.IMAGE_ERROR, 0)

    override fun onCancel(request: ImageRequest) = timing.mark(Event.IMAGE_CANCEL, 0)
}
