package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.ImageQualityControl
import com.solthariel.pawchan.domain.settings.AttachmentDefaults
import com.solthariel.pawchan.domain.settings.ReencodeFormat

internal fun imageEncodingGroup(
    files: AttachmentDefaults,
    change: ((AttachmentDefaults) -> AttachmentDefaults) -> Unit,
): SettingsGroup =
    SettingsGroup(
        "Перекодирование изображений",
        buildList {
            add(
                SettingsItem.Choice(
                    "Формат",
                    listOf("Не перекодировать", "JPEG", "PNG"),
                    files.reencoding.format.ordinal,
                ) { index ->
                    change { it.copy(reencoding = it.reencoding.copy(format = ReencodeFormat.entries[index])) }
                },
            )
            if (files.reencoding.format != ReencodeFormat.ORIGINAL) {
                if (files.reencoding.format == ReencodeFormat.JPEG) {
                    add(
                        SettingsItem.Custom("Качество JPEG") {
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                ImageQualityControl(files.reencoding.quality, true) { quality ->
                                    change { it.copy(reencoding = it.reencoding.copy(quality = quality)) }
                                }
                            }
                        },
                    )
                }
                val divisors = IMAGE_DIVISORS
                add(
                    SettingsItem.Choice(
                        "Размер изображения",
                        listOf("Исходный", "В 2 раза меньше", "В 4 раза меньше", "В 8 раз меньше"),
                        divisors.indexOf(files.reencoding.divisor).coerceAtLeast(0),
                    ) { index ->
                        change { it.copy(reencoding = it.reencoding.copy(divisor = divisors[index])) }
                    },
                )
            }
        },
    )

@Composable
internal fun UploadNameSetting(
    value: String,
    onChange: (String) -> Unit,
) {
    var text by rememberSaveable(value) { mutableStateOf(value) }
    val valid = validUploadBase(text)
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            if (validUploadBase(it)) onChange(it)
        },
        label = { Text("Имя без расширения") },
        supportingText = {
            Text(
                if (valid) {
                    "Расширение добавляется по формату файла"
                } else {
                    "От 1 до 120 символов, без /, \\ и управляющих символов"
                },
            )
        },
        isError = !valid,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}

internal fun validUploadBase(value: String): Boolean =
    value.isNotBlank() && value.length <= MAX_UPLOAD_BASE_LENGTH &&
        value.none { it == '/' || it == '\\' || it.isISOControl() }

private const val MAX_UPLOAD_BASE_LENGTH = 120
private const val QUARTER_DIVISOR = 4
private const val EIGHTH_DIVISOR = 8
private val IMAGE_DIVISORS = listOf(1, 2, QUARTER_DIVISOR, EIGHTH_DIVISOR)
