package com.solthariel.pawchan.media.video

/** Empty surfaces remain hidden independently of the availability of their thumbnail. */
internal fun videoSurfaceAlpha(hasVideoOutput: Boolean): Float = if (hasVideoOutput) 1f else 0f
