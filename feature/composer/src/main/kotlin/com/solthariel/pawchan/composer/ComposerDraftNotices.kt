package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ComposerDraftNotices(drafts: ComposerDraftCoordinator) {
    val notice by drafts.notices.collectAsStateWithLifecycle()
    val host = remember { SnackbarHostState() }
    LaunchedEffect(notice) { notice?.let { host.showSnackbar(it.message) } }
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(host, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp))
    }
}
