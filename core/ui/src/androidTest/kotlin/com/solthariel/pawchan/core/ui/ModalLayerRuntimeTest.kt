package com.solthariel.pawchan.core.ui

import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Device tests: real native windows, not the host-side state-model checks. */
class ModalLayerRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nestedDialogDoesNotDimThePageTwiceAndDismissRestoresIt() {
        checkNestedScrim(ModalScrimStrength.STANDARD, 0.4f)
    }

    @Test fun mediaBackdropUsesNinetyPercentWithoutMultiplyingOnNesting() {
        checkNestedScrim(ModalScrimStrength.MEDIA, 0.1f)
    }

    private fun checkNestedScrim(
        strength: ModalScrimStrength,
        remainingLight: Float,
    ) {
        val parent = mutableStateOf(false)
        val child = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                SafeAreaModalHost {
                    Box(Modifier.fillMaxSize().background(Color.White)) {
                        TextButton({ parent.value = true }) { Text("Open parent") }
                    }
                    if (parent.value) {
                        ModalWindow({ parent.value = false }, strength = strength) {
                            TextButton({ child.value = true }) { Text("Open child") }
                        }
                    }
                    if (child.value) {
                        AppDialog({ child.value = false }) {
                            TextButton({ child.value = false }) { Text("Close child") }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Open parent").performClick()
        val first = pagePixel()
        assertEquals(remainingLight, first.red, 0.02f)
        compose.onNodeWithText("Open child").performClick()
        assertEquals(first, pagePixel())
        compose.onNodeWithText("Close child").performClick()
        assertEquals(first, pagePixel())
        compose.runOnIdle { parent.value = false }
        assertEquals(1f, pagePixel().red, 0.01f)
    }

    @Test fun managedWindowClearsOnlyPlatformDimAndPreservesSecurePolicy() {
        lateinit var window: Window
        compose.setContent {
            MaterialTheme {
                SafeAreaModalHost {
                    AppDialog({}, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)) {
                        val view = LocalView.current
                        SideEffect { window = (view.parent as DialogWindowProvider).window }
                        Text("Secure modal")
                    }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(0f, window.attributes.dimAmount, 0f)
            assertEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            assertEquals(false, window.isNavigationBarContrastEnforced)
        }
    }

    @Test fun dropdownUsesTheSamePageScrimAndRestoresOnDismiss() {
        val open = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                SafeAreaModalHost {
                    Box(Modifier.fillMaxSize().background(Color.White)) {
                        TextButton({ open.value = true }) { Text("Open menu") }
                        ModalDropdownMenu(open.value, { open.value = false }) {
                            TextButton({ open.value = false }) { Text("Close menu") }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Open menu").performClick()
        assertEquals(0.4f, pagePixel().red, 0.02f)
        compose.onNodeWithText("Close menu").performClick()
        assertEquals(1f, pagePixel().red, 0.01f)
    }

    /** Keep screenshot probes outside Android 15 system navigation contrast scrims. */
    @Composable
    private fun SafeAreaModalHost(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            ModalLayerHost(content)
        }
    }

    private fun pagePixel(): Color {
        compose.waitForIdle()
        val pixels = compose.onNodeWithTag("modal-layer-host", useUnmergedTree = true).captureToImage().toPixelMap()
        return pixels[pixels.width - 2, pixels.height - 2]
    }
}
