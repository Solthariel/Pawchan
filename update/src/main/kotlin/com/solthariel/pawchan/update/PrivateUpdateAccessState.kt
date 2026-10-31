package com.solthariel.pawchan.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.GeneralSecurityException

/** Token input is deliberately not saveable, logged or included in a data-class toString. */
internal class PrivateUpdateAccessState(
    private val store: UpdateTokenStore,
    private val scope: CoroutineScope,
    private val onChanged: (Boolean) -> Unit,
) {
    var token by mutableStateOf("")
    var configured by mutableStateOf(false)
        private set
    var busy by mutableStateOf(true)
        private set
    var error by mutableStateOf(false)
        private set

    suspend fun load() {
        refresh { !store.read().isNullOrEmpty() }
    }

    fun save() {
        val entered = token.trim()
        if (!validUpdateToken(entered)) {
            error = true
            return
        }
        modify { store.save(entered) }
    }

    fun clear() = modify(store::clear)

    private fun modify(action: () -> Unit) {
        if (busy) return
        busy = true
        error = false
        scope.launch {
            val success =
                refresh {
                    action()
                    !store.read().isNullOrEmpty()
                }
            if (success) {
                token = ""
                onChanged(configured)
            }
        }
    }

    private suspend fun refresh(action: () -> Boolean): Boolean =
        try {
            configured = withContext(Dispatchers.IO) { action() }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            error = true
            false
        } catch (_: GeneralSecurityException) {
            error = true
            false
        } catch (_: IllegalArgumentException) {
            error = true
            false
        } catch (_: IllegalStateException) {
            error = true
            false
        } catch (_: SecurityException) {
            error = true
            false
        } finally {
            busy = false
        }
}
