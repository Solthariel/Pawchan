package com.solthariel.pawchan.domain.filter

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/** Lifecycle-owned timer; a changed rules list cancels its previous expiration wait. */
fun Flow<List<ContentFilterRule>>.activeContentFilters(): Flow<List<ContentFilterRule>> =
    channelFlow {
        this@activeContentFilters.collectLatest { rules ->
            var waiting = true
            while (waiting) {
                val now = System.currentTimeMillis()
                val active =
                    rules.filter {
                        it.enabled && it.mode.isCurrentFilterMode &&
                            (it.options.expiresAtMillis == null || now < it.options.expiresAtMillis)
                    }
                send(active)
                val next = active.mapNotNull { it.options.expiresAtMillis }.minOrNull()
                if (next == null) waiting = false else delay((next - now).coerceAtLeast(1L))
            }
        }
    }.distinctUntilChanged()
