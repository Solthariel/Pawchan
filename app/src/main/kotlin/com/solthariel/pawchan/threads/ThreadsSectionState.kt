package com.solthariel.pawchan.threads

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/** Какие секции свёрнуты — запоминается между запусками. */
internal class ThreadsSectionState(
    private val context: Context,
) {
    var collapsed by mutableStateOf(load())
        private set

    var unfolded by mutableStateOf(emptySet<ThreadsSection>())
        private set

    fun toggleTail(section: ThreadsSection) {
        unfolded = if (section in unfolded) unfolded - section else unfolded + section
    }

    fun toggle(section: ThreadsSection) {
        collapsed = if (section in collapsed) collapsed - section else collapsed + section
        preferences().edit { putStringSet(COLLAPSED_KEY, collapsed.map { it.name }.toSet()) }
    }

    fun expand(section: ThreadsSection) {
        collapsed = collapsed - section
    }

    private fun load(): Set<ThreadsSection> =
        preferences()
            .getStringSet(COLLAPSED_KEY, emptySet())
            .orEmpty()
            .mapNotNull { name -> ThreadsSection.entries.firstOrNull { it.name == name } }
            .toSet()

    private fun preferences() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}

@Composable
internal fun rememberThreadsSectionState(): ThreadsSectionState {
    val context = LocalContext.current.applicationContext
    return remember(context) { ThreadsSectionState(context) }
}

private const val PREFERENCES_NAME = "threads_hub"
private const val COLLAPSED_KEY = "collapsed"
