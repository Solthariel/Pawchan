package com.solthariel.pawchan.domain.settings

/** Defaults for new interactions; existing drafts retain their per-file choices. */
data class ClientPreferences(
    val composer: ComposerPreferences = ComposerPreferences(),
    val media: MediaPreferences = MediaPreferences(),
    val reading: ReadingPreferences = ReadingPreferences(),
    val automation: AutomationPreferences = AutomationPreferences(),
    val downloads: DownloadPreferences = DownloadPreferences(),
    val storage: StoragePreferences = StoragePreferences(),
    val mirror: String = "2ch.su",
)

enum class ComposerField { NAME, EMAIL, SUBJECT, TAG }

enum class UploadNameMode { ORIGINAL, REMOVE, RANDOM, CUSTOM }

enum class ReencodeFormat { ORIGINAL, JPEG, PNG }

data class ComposerPreferences(
    val hiddenFields: Set<ComposerField> = emptySet(),
    val attachments: AttachmentDefaults = AttachmentDefaults(),
)

data class AttachmentDefaults(
    val uniqueHash: Boolean = false,
    val removeMetadata: Boolean = false,
    val spoiler: Boolean = false,
    val naming: UploadNaming = UploadNaming(),
    val reencoding: ImageEncodingDefaults = ImageEncodingDefaults(),
)

data class UploadNaming(
    val mode: UploadNameMode = UploadNameMode.ORIGINAL,
    val customBase: String = "image",
)

data class ImageEncodingDefaults(
    val format: ReencodeFormat = ReencodeFormat.ORIGINAL,
    val quality: Int = 100,
    val divisor: Int = 1,
)

enum class VideoCompletion { STOP, REPEAT }

data class MediaPreferences(
    val playAfterScroll: Boolean = true,
    val syncThreadPosition: Boolean = false,
    val completion: VideoCompletion = VideoCompletion.STOP,
    val clearCacheOnExit: Boolean = false,
    val preloadVideo: Boolean = false,
    val preloadImages: ImagePreload = ImagePreload.ALWAYS,
)

data class ReadingPreferences(
    val restoreLastScreen: Boolean = true,
    val rememberPosition: Boolean = true,
    val navigation: NavigationPreferences = NavigationPreferences(),
)
