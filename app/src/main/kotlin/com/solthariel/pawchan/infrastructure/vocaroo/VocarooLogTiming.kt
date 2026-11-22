package com.solthariel.pawchan.infrastructure.vocaroo

internal const val TAG = "Vocaroo"

internal fun elapsedMillis(startedNanos: Long): Long = (System.nanoTime() - startedNanos) / NANOS_IN_MILLI

private const val NANOS_IN_MILLI = 1_000_000L
