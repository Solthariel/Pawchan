package com.solthariel.pawchan.infrastructure

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Правила куки 2ch по имени: не принимать и не отправлять, удалять при выходе (и при следующем запуске). */
internal data class CookieRules(
    val blocked: Set<String> = emptySet(),
    val deleteOnExit: Set<String> = emptySet(),
) {
    fun withBlocked(
        name: String,
        on: Boolean,
    ): CookieRules = copy(blocked = if (on) blocked + name else blocked - name)

    fun withDeleteOnExit(
        name: String,
        on: Boolean,
    ): CookieRules = copy(deleteOnExit = if (on) deleteOnExit + name else deleteOnExit - name)
}

/** Одно хранилище на процесс: его видят все клиенты 2ch, включая фоновые задачи. */
internal class CookieRuleStore(
    initial: CookieRules,
    private val save: (CookieRules) -> Unit,
) {
    private val mutable = MutableStateFlow(initial)
    val rules: StateFlow<CookieRules> = mutable.asStateFlow()

    fun current(): CookieRules = mutable.value

    fun update(change: (CookieRules) -> CookieRules) {
        val next = change(mutable.value)
        mutable.value = next
        save(next)
    }

    companion object {
        @Volatile var shared: CookieRuleStore? = null
            private set

        fun get(context: Context): CookieRuleStore =
            shared ?: synchronized(this) {
                shared ?: create(context.applicationContext).also { shared = it }
            }

        private fun create(context: Context): CookieRuleStore {
            val preferences = context.getSharedPreferences(COOKIE_RULES_FILE, Context.MODE_PRIVATE)
            val initial =
                CookieRules(
                    preferences.getStringSet(BLOCKED_KEY, null).orEmpty().toSet(),
                    preferences.getStringSet(DELETE_ON_EXIT_KEY, null).orEmpty().toSet(),
                )
            return CookieRuleStore(initial) { rules ->
                preferences.edit {
                    putStringSet(BLOCKED_KEY, rules.blocked)
                    putStringSet(DELETE_ON_EXIT_KEY, rules.deleteOnExit)
                }
            }
        }
    }
}

/** Куки, которые можно принять или отправить при этих правилах. */
internal fun <T> List<T>.allowedBy(
    rules: CookieRules,
    name: (T) -> String,
): List<T> = filterNot { name(it) in rules.blocked }

private const val COOKIE_RULES_FILE = "cookie_rules"
private const val BLOCKED_KEY = "dvach_blocked"
private const val DELETE_ON_EXIT_KEY = "dvach_delete_on_exit"
