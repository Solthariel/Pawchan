package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.data.network.proxy.ProxyFailure
import com.solthariel.pawchan.data.network.proxy.ProxyProbeResult
import com.solthariel.pawchan.data.network.proxy.proxyRouteFor
import com.solthariel.pawchan.domain.proxy.SavedProxy
import com.solthariel.pawchan.domain.proxy.validProxyCredentials
import com.solthariel.pawchan.infrastructure.LocalProxyCatalog
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ProxyDraftChecks(
    draft: SavedProxy,
    mirror: String,
) {
    val res = LocalResources.current

    val runtime = LocalProxyCatalog.current ?: return
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var open by remember { mutableStateOf(false) }
    Button(
        onClick = {
            focus.clearFocus()
            keyboard?.hide()
            open = true
        },
        enabled = canCheckProxy(draft),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 56.dp),
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) { Text(res.getString(L.ui_check_selected)) }
    if (open) {
        ProxyCheckDialog(draft, mirror, runtime::check) { open = false }
    }
}

internal fun canCheckProxy(draft: SavedProxy): Boolean =
    draft.services.isNotEmpty() && proxyRouteFor(draft.mode, draft.address, draft.credentials) != null &&
        (draft.credentials?.let { validProxyCredentials(it, draft.mode) } ?: true)

internal fun proxyProbeLabel(result: ProxyProbeResult): String =
    when (result) {
        is ProxyProbeResult.Response -> {
            if (result.code in HTTP_SUCCESS_FIRST..HTTP_SUCCESS_LAST) {
                "Ответ за ${result.elapsedMillis} мс"
            } else {
                "HTTP ${result.code}, ${result.elapsedMillis} мс"
            }
        }

        ProxyProbeResult.Unavailable -> {
            "Прокси недоступен или настройки изменились"
        }

        is ProxyProbeResult.Failed -> {
            proxyFailureLabel(result.reason)
        }
    }

private const val HTTP_SUCCESS_FIRST = 200
private const val HTTP_SUCCESS_LAST = 299

private fun proxyFailureLabel(reason: ProxyFailure): String =
    when (reason) {
        ProxyFailure.TIMEOUT -> "Время ожидания истекло"
        ProxyFailure.PROXY_UNREACHABLE -> "Не удалось подключиться к прокси"
        ProxyFailure.AUTH_REQUIRED -> "Прокси требует логин и пароль"
        ProxyFailure.AUTH_REJECTED -> "Прокси отклонил логин или пароль"
        ProxyFailure.TLS_UNTRUSTED -> "Сертификат прокси недействителен или ему нет доверия"
        ProxyFailure.TLS_HOSTNAME -> "Сертификат прокси не соответствует адресу"
        ProxyFailure.TLS_FAILED -> "Не удалось установить TLS с прокси"
        ProxyFailure.BAD_PROXY_RESPONSE -> "Прокси не поддерживает выбранный протокол"
        ProxyFailure.TARGET_UNREACHABLE -> "Прокси не смог подключиться к ресурсу"
        ProxyFailure.RESOURCE_TLS -> "Ошибка TLS ресурса"
        ProxyFailure.NOT_READY -> "Прокси ещё не готов"
        ProxyFailure.OTHER -> "Не удалось подключиться через прокси"
    }
