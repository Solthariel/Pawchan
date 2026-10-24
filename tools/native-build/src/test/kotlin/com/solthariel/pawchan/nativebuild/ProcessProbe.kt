package com.solthariel.pawchan.nativebuild

import kotlin.system.exitProcess

/** A real JVM child process for portable process-lifecycle tests. */
internal object ProcessProbe {
    @JvmStatic
    fun main(arguments: Array<String>) {
        when (arguments[0]) {
            "echo" -> {
                print(arguments[1])
            }

            "fail" -> {
                print("failure")
                exitProcess(7)
            }

            "sleep" -> {
                Thread.sleep(30_000)
            }

            else -> {
                error("Unknown probe command")
            }
        }
    }
}
