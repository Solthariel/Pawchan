package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun LicenseInventoryContent(uriHandler: UriHandler) {
    val assets = LocalContext.current.applicationContext.assets
    val inventory by produceState<List<LicensedComponent>?>(null, assets) {
        value =
            withContext(Dispatchers.IO) {
                try {
                    assets.open("licenses/THIRD-PARTY-NOTICES.txt").bufferedReader().use {
                        parseLicenseInventory(it.readLines())
                    }
                } catch (_: IOException) {
                    emptyList()
                }
            }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(LICENSE_DOCUMENT_GROUPS, key = { it.key }) { (title, documents) ->
            LicenseDocumentRow(title, documents, uriHandler)
        }
        val entries = inventory
        if (entries == null) {
            item { Text("Загрузка…", Modifier.padding(20.dp)) }
        } else if (entries.isEmpty()) {
            item { Text("Не удалось прочитать список зависимостей.", Modifier.padding(20.dp)) }
        } else {
            items(entries, key = { it.coordinate }) { component -> LicenseComponentRow(component, uriHandler) }
        }
    }
}

@Composable
private fun LicenseDocumentRow(
    title: String,
    documents: List<LicenseDocument>,
    uriHandler: UriHandler,
) {
    Column {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            documents.forEach { document ->
                LicenseDescription(document.summary, Modifier.padding(top = 12.dp))
            }
            documents.mapNotNull { it.source }.distinct().forEach { source ->
                LicenseSourceLink(source) { uriHandler.openUri(source) }
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp))
    }
}

@Composable
private fun LicenseComponentRow(
    component: LicensedComponent,
    uriHandler: UriHandler,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            component.coordinate.substringAfter(':').substringBefore(':'),
            style = MaterialTheme.typography.bodyLarge,
        )
        component.licenses.forEach { license ->
            LicenseDescription(license.name.licensedUnder(), Modifier.padding(top = 12.dp))
            if (license.url.startsWith("https://") || license.url.startsWith("http://")) {
                LicenseSourceLink(license.url) { uriHandler.openUri(license.url) }
            }
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun LicenseDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier,
        style =
            MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

@Composable
private fun LicenseSourceLink(
    source: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
    ) {
        Text(
            source,
            maxLines = 2,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.Underline,
                ),
        )
    }
}

private fun String.licensedUnder(): String =
    if (startsWith("The ", ignoreCase = true)) {
        "Licensed under $this"
    } else {
        "Licensed under the $this"
    }

private val LICENSE_DOCUMENT_GROUPS = LICENSE_DOCUMENTS.groupBy { it.title }.entries.toList()
