package com.solthariel.pawchan.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** The same section and entry components are used by the lazy installed-release history. */
@Composable
public fun ReleaseNotesContent(
    source: String,
    modifier: Modifier = Modifier,
) {
    val sections = remember(source) { parseReleaseNotes(source) }
    Column(
        modifier = modifier.fillMaxWidth().testTag("update-release-notes"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        sections.forEach { section ->
            ReleaseNotesHeading(section.title)
            section.entries.forEach { ReleaseNotesEntry(it) }
        }
    }
}

@Composable
public fun ReleaseNotesHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        title,
        modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
public fun ReleaseNotesEntry(
    entry: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(entry, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}
