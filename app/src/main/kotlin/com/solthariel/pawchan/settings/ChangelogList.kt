package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.BuildConfig

@Composable
internal fun ChangelogList(
    releases: List<ChangelogRelease>,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().testTag("changelog-list"),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        releases.forEach { release ->
            item(key = "version-${release.code}", contentType = "version") {
                ChangelogReleaseHeading(release)
            }
            release.sections.forEachIndexed { groupIndex, section ->
                item(key = "heading-${release.code}-$groupIndex", contentType = "heading") {
                    Text(
                        section.title,
                        Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(
                    section.entries.size,
                    key = { "entry-${release.code}-$groupIndex-$it" },
                    contentType = { "entry" },
                ) { index ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(section.entries[index], style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogReleaseHeading(release: ChangelogRelease) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("changelog-version-${release.code}"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            release.version,
            Modifier.alignByBaseline().testTag("changelog-version-label-${release.code}"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            "·",
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            release.date,
            Modifier.alignByBaseline().testTag("changelog-date-${release.code}"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        if (release.code == BuildConfig.VERSION_CODE) {
            Text(
                "·",
                Modifier.alignByBaseline(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Установлена",
                Modifier.alignByBaseline().testTag("changelog-installed-${release.code}"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
