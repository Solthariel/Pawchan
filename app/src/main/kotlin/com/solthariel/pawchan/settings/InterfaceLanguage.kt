package com.solthariel.pawchan.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.solthariel.pawchan.R
import com.solthariel.pawchan.locale.PawchanLocales

@Composable
internal fun interfaceLanguageGroup(): SettingsGroup {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val tags = listOf("") + PawchanLocales.tags
    val options = listOf(stringResource(R.string.theme_system)) + LANGUAGE_NAMES
    return SettingsGroup(
        stringResource(R.string.language_group),
        listOf(
            SettingsItem.Choice(
                stringResource(R.string.interface_language),
                options,
                tags.indexOf(PawchanLocales.selected(context)).coerceAtLeast(0),
            ) { index -> activity?.let { PawchanLocales.choose(it, tags[index]) } },
        ),
    )
}

private val LANGUAGE_NAMES =
    listOf(
        "Русский",
        "English",
        "Deutsch",
        "Português (Brasil)",
        "Español",
        "Français",
        "Italiano",
        "Polski",
        "Suomi",
        "日本語",
    )
