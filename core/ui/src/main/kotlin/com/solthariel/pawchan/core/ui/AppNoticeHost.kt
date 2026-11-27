package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val LocalAppNotices = staticCompositionLocalOf<AppNoticeState?> { null }

/** Window-owned overlay; the content is measured independently of the notice. */
@Composable
fun AppNoticeHost(content: @Composable () -> Unit) {
    val state = remember { AppNoticeState() }
    CompositionLocalProvider(LocalAppNotices provides state) {
        Box(Modifier.fillMaxSize()) {
            content()
            state.current?.let { notice ->
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .systemBarsPadding()
                        .padding(
                            top = APP_TOP_BAR_HEIGHT + 8.dp,
                            bottom = notice.bottomInset,
                        ),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    AppNoticePill(notice, Modifier.heightIn(max = maxHeight / 2).padding(horizontal = 12.dp)) {
                        state.dismiss(notice)
                    }
                }
                NoticeTimeout(notice) { state.dismiss(notice) }
            }
        }
    }
}

/** Emits once per event, and takes no layout space even inside a Column or LazyColumn item. */
@Composable
fun TransientNotice(
    message: String?,
    eventKey: Any? = message,
    action: NoticeAction? = null,
    bottomInset: Dp? = null,
    compactAction: Boolean = false,
    onDismiss: () -> Unit = {},
) {
    val state = LocalAppNotices.current
    val inset = bottomInset ?: (LocalBottomOverlayInset.current + 12.dp)
    val notice =
        remember(message, eventKey, compactAction) {
            message?.takeIf(String::isNotBlank)?.let { AppNotice(it, action, inset, compactAction, onDismiss) }
        }
    SideEffect { if (notice != null) notice.bottomInset = inset }
    LaunchedEffect(state, notice) { if (notice != null) state?.show(notice) }
    DisposableEffect(state, notice) {
        onDispose { if (notice != null) state?.dismiss(notice) }
    }
}

/** Compact update: only its action responds to taps; the message closes on a timer. */
@Composable
fun CompactStatusNotice(
    message: String?,
    eventKey: Any?,
    action: NoticeAction?,
    bottomInset: Dp,
    onDismiss: () -> Unit,
) {
    val state = LocalAppNotices.current
    val notice =
        remember(message, eventKey) {
            message?.takeIf(String::isNotBlank)?.let {
                AppNotice(it, action, bottomInset, compactAction = true, onDismiss = onDismiss).apply {
                    timeoutMillis = COMPACT_STATUS_TIMEOUT_MILLIS
                    actionOnly = true
                }
            }
        }
    SideEffect { if (notice != null) notice.bottomInset = bottomInset }
    LaunchedEffect(state, notice) { if (notice != null) state?.show(notice) }
    DisposableEffect(state, notice) {
        onDispose { if (notice != null) state?.dismiss(notice) }
    }
}

@Composable
private fun NoticeTimeout(
    notice: AppNotice,
    dismiss: () -> Unit,
) {
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(notice) {
        val timeout =
            accessibility?.calculateRecommendedTimeoutMillis(
                notice.timeoutMillis ?: noticeReadingMillis(notice.message.length),
                containsText = true,
                containsControls = true,
            ) ?: (notice.timeoutMillis ?: noticeReadingMillis(notice.message.length))
        delay(timeout)
        dismiss()
    }
}

@Composable
private fun AppNoticePill(
    notice: AppNotice,
    modifier: Modifier,
    dismiss: () -> Unit,
) {
    val surfaceModifier =
        modifier
            .widthIn(max = 480.dp)
            .testTag("app-notice")
            .semantics { liveRegion = LiveRegionMode.Polite }
    val content: @Composable () -> Unit = {
        if (notice.compactAction) {
            CompactNoticeContent(notice, dismiss)
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
                Text(modelLabel(notice.message), style = MaterialTheme.typography.bodyMedium)
                notice.action?.let { action ->
                    TextButton(onClick = {
                        dismiss()
                        action.onClick()
                    }, modifier = Modifier.align(Alignment.End)) {
                        Text(modelLabel(action.label))
                    }
                }
            }
        }
    }
    if (notice.actionOnly) {
        Surface(
            modifier = surfaceModifier,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 6.dp,
            content = content,
        )
    } else {
        Surface(
            onClick = {
                dismiss()
                if (notice.compactAction) notice.action?.onClick?.invoke()
            },
            modifier = surfaceModifier,
            shape = if (notice.compactAction) CircleShape else RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 6.dp,
            content = content,
        )
    }
}

@Composable
private fun CompactNoticeContent(
    notice: AppNotice,
    dismiss: () -> Unit,
) {
    Row(Modifier.height(40.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(modelLabel(notice.message), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        notice.action?.let { action ->
            VerticalDivider(
                Modifier.padding(horizontal = 12.dp).height(16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                modelLabel(action.label),
                modifier =
                    if (notice.actionOnly) {
                        Modifier.clickable {
                            dismiss()
                            action.onClick()
                        }
                    } else {
                        Modifier
                    },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private const val COMPACT_STATUS_TIMEOUT_MILLIS = 3_500L
