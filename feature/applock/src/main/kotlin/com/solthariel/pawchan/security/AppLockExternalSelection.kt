package com.solthariel.pawchan.security

/** One host-launched external selection and its matching result, not a general unlock grant. */
interface AppLockExternalSelection {
    fun begin()

    fun returned()

    fun cancel()
}
