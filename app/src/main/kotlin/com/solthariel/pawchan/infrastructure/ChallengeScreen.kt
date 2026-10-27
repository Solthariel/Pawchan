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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.solthariel.pawchan.R

@Composable
internal fun ChallengeScreen(
    url: String?,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val allowed = com.solthariel.pawchan.infrastructure.sites.LocalForumMirror.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        TextButton(onClick = onDone) {
            Text(stringResource(R.string.challenge_done))
        }
        HorizontalDivider()
        if (url != null && isAllowedChallengeUri(url.toUri(), allowed)) {
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
