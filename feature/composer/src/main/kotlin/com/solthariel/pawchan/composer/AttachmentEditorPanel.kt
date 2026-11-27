package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.feature.composer.R
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun AttachmentEditorPanel(
    file: ComposerAttachment,
    draft: AttachmentViewerDraft,
    dimensions: ReencodingDimensions?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    LaunchedEffect(draft.tab) { focus.clearFocus() }
    val tab = draft.tab.takeIf { dimensions != null || it == AttachmentEditorTab.NAME } ?: AttachmentEditorTab.FILE
    Surface(
        modifier = modifier.testTag("attachment-panel"),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                // The 5dp bottom inset plus the action row's 4dp top inset gives the 9dp target gap.
                // Start each tool at the bottom on its first layout, without a post-layout scroll.
                key(tab) {
                    val settingsScroll = rememberScrollState()
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .testTag("attachment-editor-settings")
                            .verticalScroll(settingsScroll, reverseScrolling = true)
                            .heightIn(min = maxHeight)
                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 5.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
                    ) {
                        AttachmentEditorSettings(file, draft, dimensions, enabled)
                    }
                }
            }
            if (tab == AttachmentEditorTab.FILE || tab == AttachmentEditorTab.NAME) {
                AttachmentOptionsPanel(
                    file,
                    enabled,
                    onEditorAction = { label ->
                        draft.selectTab(
                            if (label == R.string.attachment_rename) {
                                if (draft.tab == AttachmentEditorTab.NAME) {
                                    AttachmentEditorTab.FILE
                                } else {
                                    AttachmentEditorTab.NAME
                                }
                            } else if (label == R.string.attachment_edit_image) {
                                AttachmentEditorTab.CROP
                            } else {
                                AttachmentEditorTab.FILE
                            },
                        )
                    },
                    editorState =
                        AttachmentOptionsEditorState(
                            imageEditEnabled = dimensions != null,
                            namePanelOpen = tab == AttachmentEditorTab.NAME,
                        ),
                ) { draft.options = it.options }
            } else {
                AttachmentEditorTabs(draft, enabled)
            }
        }
    }
}

@Composable
private fun AttachmentEditorSettings(
    file: ComposerAttachment,
    draft: AttachmentViewerDraft,
    dimensions: ReencodingDimensions?,
    enabled: Boolean,
) {
    val res = LocalResources.current

    val tab = draft.tab.takeIf { dimensions != null || it == AttachmentEditorTab.NAME } ?: AttachmentEditorTab.FILE
    val interactive = enabled && !draft.editor.inGesture && !draft.comparing
    when (tab) {
        AttachmentEditorTab.FILE -> {
            if (file.canReencode) {
                AttachmentReencodingSummary(file, file.options.reencoding)
                if (dimensions == null) {
                    Text(
                        res.getString(L.ui_the_editor_is_available_for),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else if (file.video) {
                AttachmentFileSummary(file)
            } else {
                Text(file.displayName, maxLines = 1)
                Text(file.typeAndSize, style = MaterialTheme.typography.bodySmall)
            }
        }

        AttachmentEditorTab.CROP -> {
            ImageAspectRatioControls(draft, requireNotNull(dimensions), interactive)
        }

        AttachmentEditorTab.ROTATE -> {
            ImageRotationControls(draft, interactive)
        }

        AttachmentEditorTab.NAME -> {
            AttachmentNamePanel(file, draft, enabled)
        }

        AttachmentEditorTab.DRAW -> {
            ImageDrawingToolbar(draft.tools, interactive)
        }

        AttachmentEditorTab.REDACT -> {
            ImageRedactionToolbar(draft.tools, interactive)
        }
    }
}
