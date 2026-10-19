package com.solthariel.pawchan.settings

import com.solthariel.pawchan.update.ReleaseNotesSection
import com.solthariel.pawchan.update.parseReleaseNotes

internal typealias ChangelogSection = ReleaseNotesSection

internal fun parseChangelog(source: String): List<ChangelogSection> = parseReleaseNotes(source)
