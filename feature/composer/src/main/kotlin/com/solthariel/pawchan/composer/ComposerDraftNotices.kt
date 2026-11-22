package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.TransientNotice

@Composable
fun ComposerDraftNotices(drafts: ComposerDraftCoordinator) {
    val notice by drafts.notices.collectAsStateWithLifecycle()
    TransientNotice(notice?.message, eventKey = notice)
}
