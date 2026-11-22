package com.solthariel.pawchan.arhivach

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.archive.ArchiveTag

@Composable
internal fun ArchivePageMenu(
    state: ArchiveBrowseState,
    model: ArchiveBrowseModel,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    var jumpDialogVisible by rememberSaveable { mutableStateOf(false) }
    val currentPage = state.page + 1
    val totalPages = state.list?.totalPages
    DropdownMenu(
        expanded = visible,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 200.dp, max = 320.dp),
    ) {
        ArchiveCurrentPageItem(currentPage, totalPages, state.load.loading) {
            onDismiss()
            jumpDialogVisible = true
        }
        ArchivePageTagItems(state.tags, model, onDismiss)
    }
    if (jumpDialogVisible) {
        ArchivePageJumpDialog(
            currentPage = currentPage,
            totalPages = totalPages,
            onDismiss = { jumpDialogVisible = false },
            onConfirm = { page ->
                jumpDialogVisible = false
                if (page != currentPage) model.loadPage(page - 1)
            },
        )
    }
}

@Composable
private fun ArchiveCurrentPageItem(
    currentPage: Int,
    totalPages: Int?,
    loading: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    if (loading) "Загрузка…" else "$currentPage / ${totalPages ?: "…"}",
                    modifier = Modifier.align(Alignment.Center),
                    maxLines = 1,
                    color =
                        if (loading) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (loading) FontWeight.Normal else FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                if (loading) {
                    PageLoadingIndicator(
                        description = "Загрузка страницы Архивача",
                        modifier = Modifier.align(Alignment.CenterEnd),
                        size = 18.dp,
                    )
                } else {
                    UiForwardIndicator(Modifier.align(Alignment.CenterEnd))
                }
            }
        },
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.testTag("archive-page-current"),
    )
}

@Composable
private fun ArchivePageTagItems(
    tags: List<ArchiveTag>,
    model: ArchiveBrowseModel,
    onDismiss: () -> Unit,
) {
    if (tags.isEmpty()) return
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    tags.forEach { tag ->
        val shortTag =
            tag.title
                .substringBefore(" - ")
                .trim()
                .ifBlank { tag.title }
        DropdownMenuItem(
            text = { Text("Убрать тег $shortTag", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = {
                UiIcon(UiIconKind.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
            },
            onClick = {
                onDismiss()
                model.removeTag(tag)
            },
        )
    }
}

@Composable
private fun ArchivePageJumpDialog(
    currentPage: Int,
    totalPages: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var pageText by rememberSaveable(currentPage) { mutableStateOf(currentPage.toString()) }
    val targetPage = pageText.toIntOrNull()
    val validPage = targetPage != null && targetPage > 0 && (totalPages == null || targetPage <= totalPages)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перейти к странице") },
        text = {
            OutlinedTextField(
                value = pageText,
                onValueChange = { value -> pageText = value.filter { it in '0'..'9' } },
                label = { Text("Номер страницы") },
                supportingText = { totalPages?.let { Text("Всего страниц: $it") } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.testTag("archive-page-number-input"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { targetPage?.let(onConfirm) },
                enabled = validPage,
            ) {
                Text("Перейти")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
