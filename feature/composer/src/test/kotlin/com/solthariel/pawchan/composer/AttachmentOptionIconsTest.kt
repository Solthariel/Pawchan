package com.solthariel.pawchan.composer

import com.solthariel.pawchan.feature.composer.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentOptionIconsTest {
    @Test fun onlyTheFourSelectableFileToolsUseNavigationIconTints() {
        val expectations =
            mapOf(
                R.string.attachment_unique_hash to true,
                R.string.attachment_strip_metadata to true,
                R.string.markup_spoiler to true,
                R.string.attachment_rename to true,
                R.string.attachment_edit_image to false,
                R.string.attachment_reencode to false,
                R.string.attachment_remove_name to false,
                R.string.editor_crop to false,
            )
        expectations.forEach { (label, expected) ->
            assertEquals(expected, AttachmentTileState(label, checked = false, enabled = true).iconOnlySelection)
        }
    }

    @Test fun hashFollowsUniqueHashOption() {
        assertEquals(
            R.drawable.ic_attachment_option_hash,
            attachmentTileIcon(R.string.attachment_unique_hash, false),
        )
        assertEquals(
            R.drawable.ic_attachment_option_hash_off,
            attachmentTileIcon(R.string.attachment_unique_hash, true),
        )
    }

    @Test fun metadataFollowsRemovalOption() {
        assertEquals(
            R.drawable.ic_attachment_option_metadata,
            attachmentTileIcon(R.string.attachment_strip_metadata, false),
        )
        assertEquals(
            R.drawable.ic_attachment_option_metadata_off,
            attachmentTileIcon(R.string.attachment_strip_metadata, true),
        )
    }

    @Test fun nameAndSpoilerUseOppositeVisibilityStates() {
        assertEquals(
            R.drawable.ic_attachment_option_no_name,
            attachmentTileIcon(R.string.attachment_remove_name, false),
        )
        assertEquals(
            R.drawable.ic_attachment_option_no_name_off,
            attachmentTileIcon(R.string.attachment_remove_name, true),
        )
        assertEquals(
            R.drawable.ic_attachment_option_spoiler,
            attachmentTileIcon(R.string.markup_spoiler, false),
        )
        assertEquals(
            R.drawable.ic_attachment_option_spoiler_off,
            attachmentTileIcon(R.string.markup_spoiler, true),
        )
    }

    @Test fun dialogActionsDoNotPretendToBeOffToggles() {
        for (checked in listOf(false, true)) {
            assertEquals(
                R.drawable.ic_attachment_option_reencode,
                attachmentTileIcon(R.string.attachment_reencode, checked),
            )
            assertEquals(
                R.drawable.ic_attachment_option_name,
                attachmentTileIcon(R.string.attachment_rename, checked),
            )
        }
    }
}
