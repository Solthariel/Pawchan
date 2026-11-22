package com.solthariel.pawchan.settings

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.core.ui.KeepWindowBounds
import com.solthariel.pawchan.core.ui.ModalWindow
import com.solthariel.pawchan.core.ui.focusedWindowBottomPadding
import com.solthariel.pawchan.core.ui.modalLayerSurface
import com.solthariel.pawchan.data.network.proxy.ProxyFailure
import com.solthariel.pawchan.data.network.proxy.ProxyProbeResult
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import kotlinx.coroutines.CancellationException

internal typealias ProxyCheck = suspend (SavedProxy, ProxyService, String) -> ProxyProbeResult

@Composable
internal fun ProxyCheckDialog(
    draft: SavedProxy,
    mirror: String,
    check: ProxyCheck,
    onClose: () -> Unit,
) {
    val services = remember(draft.services) { ProxyService.entries.filter { it in draft.services } }
    var attempt by remember { mutableIntStateOf(0) }
    var results by remember { mutableStateOf<Map<ProxyService, ProxyProbeResult>>(emptyMap()) }
    var current by remember { mutableStateOf<ProxyService?>(null) }
    var checking by remember { mutableStateOf(true) }
    LaunchedEffect(draft, mirror, attempt) {
        results = emptyMap()
        checking = true
        try {
            services.forEach { service ->
                current = service
                results = results + (service to checkedResource(check, draft, service, mirror))
            }
        } finally {
            current = null
            checking = false
        }
    }
    ProxyCheckModal(onClose) {
        Text(
            "Проверка прокси",
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        services.forEach { service ->
            val status =
                results[service]?.let(::proxyProbeLabel)
                    ?: if (current == service) "Проверяется…" else "Ожидает проверки"
            Text("${service.proxyLabel}: $status", style = MaterialTheme.typography.bodyMedium)
        }
        if (!checking) {
            TextButton(onClick = {
                checking = true
                attempt++
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Повторить")
            }
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
    }
}

@Composable
private fun ProxyCheckModal(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalWindow(
        onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        KeepWindowBounds()
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().pointerInput(onClose) { detectTapGestures { onClose() } })
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .focusedWindowBottomPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .testTag("proxy-check-dialog")
                        .modalLayerSurface(AppDialogStyle.shape),
                    shape = AppDialogStyle.shape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 1f),
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState(), overscrollEffect = null)
                            .pointerInput(Unit) { detectTapGestures { } }
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) { content() }
                }
            }
        }
    }
}

private suspend fun checkedResource(
    check: ProxyCheck,
    draft: SavedProxy,
    service: ProxyService,
    mirror: String,
): ProxyProbeResult =
    try {
        check(draft, service, mirror)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ProxyProbeResult.Failed(ProxyFailure.OTHER)
    }
