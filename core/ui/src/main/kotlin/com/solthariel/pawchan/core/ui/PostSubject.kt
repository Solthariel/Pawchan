package com.solthariel.pawchan.core.ui

fun visiblePostSubject(
    subject: String?,
    body: String,
): String? {
    val heading = subject?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val normalizedHeading = heading.replace(POST_SUBJECT_WHITESPACE, " ")
    val normalizedBody = body.trim().replace(POST_SUBJECT_WHITESPACE, " ")
    val repeatedPrefix =
        normalizedBody.startsWith(normalizedHeading) &&
            (
                normalizedBody.length == normalizedHeading.length ||
                    normalizedBody[normalizedHeading.length].isWhitespace()
            )
    return heading.takeUnless { repeatedPrefix }
}

private val POST_SUBJECT_WHITESPACE = Regex("[\\s\\u00a0]+")
