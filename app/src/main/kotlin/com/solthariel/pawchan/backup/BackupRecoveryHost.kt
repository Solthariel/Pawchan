package com.solthariel.pawchan.backup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.R.string as L

@Composable
internal fun BackupRecoveryHost(
    recovery: BackupRecovery,
    content: @Composable () -> Unit,
) {
    val res = LocalResources.current

    val state by recovery.state.collectAsStateWithLifecycle()
    if (state.ready) {
        content()
    } else {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column {
                        Text(state.error ?: res.getString(L.ui_checking_unfinished_operations))
                        if (state.error !=
                            null
                        ) {
                            TextButton(onClick = recovery::start) { Text(res.getString(L.ui_retry)) }
                        }
                    }
                }
            }
        }
    }
}
