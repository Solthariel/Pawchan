package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.R.string as L

/** Shared post header: identifiers and state, followed by byline and optional rating. */
@Composable
fun PostHeaderLayout(
    leading: @Composable RowScope.() -> Unit,
    byline: @Composable RowScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(POST_HEADER_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(POST_HEADER_GAP),
            verticalAlignment = Alignment.CenterVertically,
            content = byline,
        )
        trailing()
    }
}

@Composable
fun PostHeaderIndex(
    deleted: Boolean,
    index: Int,
) {
    val res = LocalResources.current

    if (deleted) {
        PostHeaderText(
            "X",
            MaterialTheme.colorScheme.error,
            Modifier.semantics { contentDescription = res.getString(L.ui_removed) },
            weight = FontWeight.Bold,
        )
    } else {
        PostHeaderText(index.toString(), MaterialTheme.colorScheme.onSurface, weight = FontWeight.Bold)
    }
}

@Composable
fun PostHeaderText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight? = null,
) {
    Text(
        text,
        modifier,
        color = color,
        fontSize = POST_HEADER_TEXT,
        lineHeight = POST_HEADER_LINE,
        fontWeight = weight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

data class PostHeaderAuthorUi(
    val name: String? = null,
    val color: Int? = null,
    val tripcode: String? = null,
)

@Composable
fun RowScope.PostHeaderAuthor(author: PostHeaderAuthorUi) {
    val name = author.name
    val trip = author.tripcode
    if (name == null && trip == null) return
    val color = author.color?.let(::Color) ?: MaterialTheme.colorScheme.onSurface
    val text =
        buildAnnotatedString {
            name?.let { withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) { append(it) } }
            if (name != null && trip != null) append(' ')
            trip?.let { withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(it) } }
        }
    Text(
        text,
        Modifier.weight(1f, fill = false),
        fontSize = POST_HEADER_TEXT,
        lineHeight = POST_HEADER_LINE,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

fun isDefaultAnonymousName(name: String): Boolean =
    name.trim().equals("Аноним", ignoreCase = true) ||
        name.trim().equals("Anonymous", ignoreCase = true)

private val POST_HEADER_GAP = 4.dp
private val POST_HEADER_TEXT = 12.sp
private val POST_HEADER_LINE = 14.sp
