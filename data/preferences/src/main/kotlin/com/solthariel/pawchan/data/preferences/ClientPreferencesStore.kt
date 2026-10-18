package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.settings.AttachmentDefaults
import com.solthariel.pawchan.domain.settings.ClientPreferences
import com.solthariel.pawchan.domain.settings.ComposerField
import com.solthariel.pawchan.domain.settings.ComposerPreferences
import com.solthariel.pawchan.domain.settings.ImageEncodingDefaults
import com.solthariel.pawchan.domain.settings.ImagePreload
import com.solthariel.pawchan.domain.settings.MediaPreferences
import com.solthariel.pawchan.domain.settings.ReadingPreferences
import com.solthariel.pawchan.domain.settings.ReencodeFormat
import com.solthariel.pawchan.domain.settings.UploadNameMode
import com.solthariel.pawchan.domain.settings.UploadNaming
import com.solthariel.pawchan.domain.settings.VideoCompletion

internal fun Preferences.readClientPreferences(): ClientPreferences =
    ClientPreferences(
        mirror =
            this[MIRROR]?.takeIf {
                com.solthariel.pawchan.domain.settings
                    .validMirrorHost(it)
            } ?: "2ch.su",
        automation = readAutomation(),
        downloads = readDownloads(),
        storage = readStorage(),
        composer =
            ComposerPreferences(
                hiddenFields =
                    (this[HIDDEN_FIELDS].orEmpty())
                        .mapNotNull { name ->
                            ComposerField.entries.firstOrNull { it.name == name }
                        }.toSet(),
                attachments = readAttachmentDefaults(),
            ),
        media =
            MediaPreferences(
                playAfterScroll = this[PLAY_AFTER_SCROLL] ?: true,
                syncThreadPosition = this[SYNC_THREAD] ?: false,
                completion =
                    VideoCompletion.entries.firstOrNull { it.name == this[COMPLETION] } ?: VideoCompletion.STOP,
                clearCacheOnExit = this[CLEAR_ON_EXIT] ?: false,
                preloadVideo = this[PRELOAD_VIDEO] ?: false,
                preloadImages =
                    ImagePreload.entries
                        .firstOrNull { it.name == this[PRELOAD_IMAGES] }
                        ?: ImagePreload.ALWAYS,
            ),
        reading =
            ReadingPreferences(
                navigation = readNavigation(),
                restoreLastScreen = this[RESTORE_SCREEN] ?: true,
                rememberPosition = this[READING_POSITION] ?: true,
            ),
    )

private fun Preferences.readAttachmentDefaults(): AttachmentDefaults =
    AttachmentDefaults(
        uniqueHash = this[UNIQUE_HASH] ?: false,
        removeMetadata = this[REMOVE_METADATA] ?: false,
        spoiler = this[SPOILER] ?: false,
        naming =
            UploadNaming(
                mode = UploadNameMode.entries.firstOrNull { it.name == this[NAME_MODE] } ?: UploadNameMode.ORIGINAL,
                customBase = this[CUSTOM_NAME] ?: "image",
            ),
        reencoding =
            ImageEncodingDefaults(
                format = ReencodeFormat.entries.firstOrNull { it.name == this[REENCODE] } ?: ReencodeFormat.ORIGINAL,
                quality = (this[QUALITY] ?: DEFAULT_IMAGE_QUALITY).coerceIn(1, MAX_IMAGE_QUALITY),
                divisor = (this[DIVISOR] ?: 1).takeIf { it in IMAGE_DIVISORS } ?: 1,
            ),
    )

internal fun MutablePreferences.writeClientPreferences(value: ClientPreferences) {
    writeNavigation(value.reading.navigation)
    this[MIRROR] = value.mirror.takeIf {
        com.solthariel.pawchan.domain.settings
            .validMirrorHost(it)
    } ?: "2ch.su"
    this[HIDDEN_FIELDS] =
        value.composer.hiddenFields
            .map { it.name }
            .toSet()
    writeAttachmentDefaults(value.composer.attachments)
    writeAutomation(value.automation)
    writeDownloads(value.downloads)
    writeStorage(value.storage)
    this[PRELOAD_IMAGES] = value.media.preloadImages.name
    this[PLAY_AFTER_SCROLL] = value.media.playAfterScroll
    this[SYNC_THREAD] = value.media.syncThreadPosition
    this[COMPLETION] = value.media.completion.name
    this[CLEAR_ON_EXIT] = value.media.clearCacheOnExit
    this[PRELOAD_VIDEO] = value.media.preloadVideo
    this[RESTORE_SCREEN] = value.reading.restoreLastScreen
    this[READING_POSITION] = value.reading.rememberPosition
}

private fun MutablePreferences.writeAttachmentDefaults(value: AttachmentDefaults) {
    this[UNIQUE_HASH] = value.uniqueHash
    this[REMOVE_METADATA] = value.removeMetadata
    this[SPOILER] = value.spoiler
    this[NAME_MODE] = value.naming.mode.name
    this[CUSTOM_NAME] = value.naming.customBase
    this[REENCODE] = value.reencoding.format.name
    this[QUALITY] = value.reencoding.quality.coerceIn(1, MAX_IMAGE_QUALITY)
    this[DIVISOR] = value.reencoding.divisor.takeIf { it in IMAGE_DIVISORS } ?: 1
}

private val HIDDEN_FIELDS = stringSetPreferencesKey("composer_hidden_fields")
private val UNIQUE_HASH = booleanPreferencesKey("attachment_unique_hash")
private val REMOVE_METADATA = booleanPreferencesKey("attachment_remove_metadata")
private val SPOILER = booleanPreferencesKey("attachment_spoiler")
private val NAME_MODE = stringPreferencesKey("attachment_name_mode")
private val CUSTOM_NAME = stringPreferencesKey("attachment_custom_name")
private val REENCODE = stringPreferencesKey("attachment_reencode")
private val QUALITY = intPreferencesKey("attachment_quality")
private val DIVISOR = intPreferencesKey("attachment_divisor")
private val PLAY_AFTER_SCROLL = booleanPreferencesKey("media_play_after_scroll")
private val SYNC_THREAD = booleanPreferencesKey("media_sync_thread")
private val COMPLETION = stringPreferencesKey("media_video_completion")
private val CLEAR_ON_EXIT = booleanPreferencesKey("media_clear_cache_on_exit")
private val PRELOAD_VIDEO = booleanPreferencesKey("media_preload_video")
private val PRELOAD_IMAGES = stringPreferencesKey("media_preload_images")
private val RESTORE_SCREEN = booleanPreferencesKey("reading_restore_screen")
private val READING_POSITION = booleanPreferencesKey("reading_position")

private const val DEFAULT_IMAGE_QUALITY = 100
private const val MAX_IMAGE_QUALITY = 100
private const val QUARTER_DIVISOR = 4
private const val EIGHTH_DIVISOR = 8
private val IMAGE_DIVISORS = listOf(1, 2, QUARTER_DIVISOR, EIGHTH_DIVISOR)

private val MIRROR = stringPreferencesKey("forum_mirror")
