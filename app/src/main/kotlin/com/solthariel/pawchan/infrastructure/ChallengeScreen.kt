package com.solthariel.pawchan.infrastructure

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.R
import com.solthariel.pawchan.data.network.proxy.ProxyRuntimeStatus
import com.solthariel.pawchan.data.network.proxy.ProxyRuntimeStatus.DIRECT
import com.solthariel.pawchan.domain.proxy.ProxyService

@Composable
internal fun ChallengeScreen(
    url: String?,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val allowed = com.solthariel.pawchan.infrastructure.sites.LocalForumMirror.current
    val proxy = LocalProxyRuntime.current
    val states by (proxy?.states ?: EMPTY_PROXY_STATES).collectAsStateWithLifecycle()
    val canOpen = challengeAllowedForProxy(states[ProxyService.DVACH]?.status)
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        TextButton(onClick = if (canOpen) onDone else onBack) {
            Text(if (canOpen) stringResource(R.string.challenge_done) else "Назад")
        }
        HorizontalDivider()
        if (!canOpen) {
            Text(
                "Проверка в браузере недоступна при включённом прокси для 2ch: WebView не использует этот SOCKS5. " +
                    "Чтобы продолжить, отключите прокси для 2ch или используйте другой способ доступа.",
                modifier = Modifier.padding(24.dp),
            )
        } else if (url != null && isAllowedChallengeUri(url.toUri(), allowed)) {
            ChallengeWebView(url, allowed)
        } else {
            Text(
                text = stringResource(R.string.challenge_invalid_url),
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Composable
private fun ColumnScope.ChallengeWebView(
    url: String,
    allowed: String,
) {
    val context = LocalContext.current
    val webView =
        remember(context, allowed) {
            WebView(context).apply {
                enableChallengeJavaScript(this)
                settings.domStorageEnabled = true
                CookieManager.getInstance().setAcceptCookie(true)
                webViewClient = ChallengeWebViewClient(allowed)
            }
        }
    LaunchedEffect(webView, url) {
        webView.loadUrl(url)
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    AndroidView(
        factory = { webView },
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f),
    )
}

private fun enableChallengeJavaScript(webView: WebView) {
    val settings = webView.settings
    val setter =
        settings.javaClass.getMethod(
            "setJavaScriptEnabled",
            Boolean::class.javaPrimitiveType,
        )
    setter.invoke(settings, true)
}

private class ChallengeWebViewClient(
    private val allowed: String,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = !isAllowedChallengeUri(request.url, allowed)
}

private fun isAllowedChallengeUri(
    uri: Uri,
    allowed: String,
): Boolean {
    val host = uri.host?.lowercase() ?: return false
    return uri.scheme == "https" && host == allowed
}

// A WebView has a separate network stack: do not open it while the app route is blocked or proxied.
internal fun challengeAllowedForProxy(status: ProxyRuntimeStatus?): Boolean = status == null || status == DIRECT

private val EMPTY_PROXY_STATES =
    kotlinx.coroutines.flow.MutableStateFlow(
        emptyMap<ProxyService, com.solthariel.pawchan.data.network.proxy.ProxyRuntimeState>(),
    )
