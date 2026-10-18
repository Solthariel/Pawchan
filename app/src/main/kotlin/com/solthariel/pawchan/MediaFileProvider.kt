package com.solthariel.pawchan

import androidx.core.content.FileProvider

/** Separate provider class prevents manifest merging with the update module's FileProvider. */
internal class MediaFileProvider : FileProvider()
