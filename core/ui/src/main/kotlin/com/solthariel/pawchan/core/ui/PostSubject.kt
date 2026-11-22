package com.solthariel.pawchan.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

/** The same size and line height for subject and body in every post. */
@Composable
fun postBodyTextStyle(textSize: Int): TextStyle =
    TextStyle(
        fontSize = textSize.sp,
        lineHeight = (textSize + POST_BODY_LINE_HEIGHT_EXTRA).sp * LocalTextLeadingMultiplier.current,
    )

@Composable
fun PostSubjectText(
    subject: String,
    textSize: Int,
) {
    val style = postBodyTextStyle(textSize)
    Text(
        subject,
        fontWeight = FontWeight.Bold,
        fontSize = style.fontSize,
        lineHeight = style.lineHeight,
    )
}

private const val POST_BODY_LINE_HEIGHT_EXTRA = 5
