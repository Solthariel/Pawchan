package com.solthariel.pawchan.composer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import coil3.ImageLoader

/** Isolated offline fixture, compiled only into the benchmark library variant. */
class ComposerPerformanceActivity : ComponentActivity() {
    private lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageLoader = ImageLoader.Builder(this).build()
        val diagnostics = intent.getBooleanExtra("paw_diag", false)
        val research = intent.getBooleanExtra("paw_research", false)
        setContent { MaterialTheme { ComposerPerformanceFixture(imageLoader, diagnostics, research) } }
    }

    override fun onDestroy() {
        imageLoader.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun ComposerPerformanceFixture(
    imageLoader: ImageLoader,
    diagnostics: Boolean,
    research: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    ObserveDiagnosticScroll(diagnostics, scroll)
    Column(Modifier.systemBarsPadding().semantics { testTagsAsResourceId = true }) {
        Button(
            onClick = {
                if (research) AlignmentResearch.toggle(!expanded)
                if (diagnostics) {
                    diagnosticTrace(if (expanded) "paw.toggle.short" else "paw.toggle.long") { expanded = !expanded }
                } else {
                    expanded = !expanded
                }
            },
            modifier = Modifier.testTag("toggle_error"),
        ) {
            Text("Toggle fixture error")
        }
        Text(if (expanded) "fixture_long" else "fixture_short", Modifier.testTag("fixture_state"))
        Box(Modifier.width(240.dp).height(320.dp).testTag("status_viewport")) {
            Box(Modifier.verticalScroll(scroll).diagnosticLayout(diagnostics, expanded, research)) {
                ComposerStatusLine(
                    ComposerCaptchaState(),
                    ComposerScreenStatus(
                        ComposerProgressState(),
                        if (expanded) LONG_FAILURE else ComposerFailureState(),
                        false,
                    ),
                    {},
                    imageLoader,
                )
            }
        }
    }
}

private val LONG_FAILURE =
    ComposerFailureState(
        ReplyError.REJECTED,
        ("Файл слишком большой: " + "подробная причина ".repeat(ERROR_DETAIL_REPEATS)).trimEnd(),
    )

private const val ERROR_DETAIL_REPEATS = 30
