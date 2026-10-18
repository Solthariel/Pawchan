package com.solthariel.pawchan.archive

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.archive.ArchiveSubmission
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * «Сохранить в Архивач»: ссылка и «свернуть» подставляются сами, пользователь вводит только капчу.
 * Справа в заголовке — новая картинка (как и нажатие на саму капчу), внизу — «Отправить» во всю ширину.
 */
@Composable
internal fun ArhivachSubmitDialog(
    controller: ArhivachController,
    address: ThreadAddress,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val form = remember { ArhivachSubmitForm() }
    LaunchedEffect(form.reload) {
        form.captcha = CaptchaState.Loading
        form.captcha = loadCaptcha(controller)?.let(CaptchaState::Ready) ?: CaptchaState.Failed
    }
    val submit = {
        form.busy = true
        form.message = null
        scope.launch {
            when (val result = controller.submit(address, form.code)) {
                is ArchiveSubmission.Accepted -> {
                    Toast.makeText(context, submittedText(result), Toast.LENGTH_SHORT).show()
                    onClose()
                }

                is ArchiveSubmission.WrongCaptcha -> {
                    form.message = result.message
                }

                is ArchiveSubmission.Rejected -> {
                    form.message = result.message
                }
            }
            form.code = ""
            form.reload++
            form.busy = false
        }
        Unit
    }
    AppDialog(onClose) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(AppDialogStyle.contentPadding)) {
            ArhivachSubmitHeader(enabled = !form.busy && form.captcha != CaptchaState.Loading) { form.reload++ }
            CaptchaImage(form.captcha, Modifier.padding(top = 12.dp)) { if (!form.busy) form.reload++ }
            OutlinedTextField(
                form.code,
                onValueChange = { form.code = it },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text("Текст с картинки") },
                singleLine = true,
                enabled = !form.busy,
            )
            form.message?.let { Text(it, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = submit,
                enabled = !form.busy && form.code.isNotBlank() && form.captcha is CaptchaState.Ready,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                if (form.busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Отправить")
                }
            }
        }
    }
}

private class ArhivachSubmitForm {
    var reload by mutableIntStateOf(0)
    var captcha by mutableStateOf<CaptchaState>(CaptchaState.Loading)
    var code by mutableStateOf("")
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
}

private sealed interface CaptchaState {
    data object Loading : CaptchaState

    data object Failed : CaptchaState

    data class Ready(
        val image: ImageBitmap,
    ) : CaptchaState
}

@Composable
private fun ArhivachSubmitHeader(
    enabled: Boolean,
    onRefresh: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Сохранить в Архивач", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        IconButton(
            onClick = onRefresh,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = "Другая картинка" },
        ) {
            val tint = MaterialTheme.colorScheme.onSurfaceVariant
            UiIcon(UiIconKind.REFRESH, if (enabled) tint else tint.copy(alpha = DISABLED_ALPHA), Modifier.size(24.dp))
        }
    }
}

private fun submittedText(result: ArchiveSubmission.Accepted): String =
    if (result.alreadyArchived) "Тред уже есть на Архиваче" else "Тред отправлен в Архивач"

@Composable
private fun CaptchaImage(
    state: CaptchaState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onRefresh),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            CaptchaState.Loading -> {
                CircularProgressIndicator()
            }

            CaptchaState.Failed -> {
                Text(
                    "Не удалось загрузить капчу.\nНажмите, чтобы повторить.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.DarkGray,
                )
            }

            is CaptchaState.Ready -> {
                Image(state.image, "Капча", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

private suspend fun loadCaptcha(controller: ArhivachController): ImageBitmap? =
    when (val result = controller.captcha()) {
        is DomainOutcome.Success -> {
            withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(result.value, 0, result.value.size)?.asImageBitmap()
            }
        }

        is DomainOutcome.Failure -> {
            null
        }
    }

private const val DISABLED_ALPHA = 0.38f
