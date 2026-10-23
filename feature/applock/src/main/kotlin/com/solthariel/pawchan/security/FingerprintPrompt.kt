package com.solthariel.pawchan.security

import android.app.Activity
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.crypto.Cipher
import kotlin.coroutines.resume

/** The request stays busy until the system sheet closes; no overlapping prompts. */
internal suspend fun authenticateFingerprint(
    activity: Activity,
    cipher: Cipher,
): Cipher? =
    suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationSignal()
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) cancellation.cancel()
            }
        val finish: (Cipher?) -> Unit = { value ->
            lifecycle?.removeObserver(observer)
            if (continuation.isActive) continuation.resume(value)
        }
        lifecycle?.addObserver(observer)
        continuation.invokeOnCancellation { cancellation.cancel() }
        val prompt =
            BiometricPrompt
                .Builder(activity)
                .setTitle("Разблокировать Pawchan")
                .setSubtitle("Подтвердите отпечаток пальца")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButton("Ввести PIN-код", activity.mainExecutor) { _, _ -> finish(null) }
                .build()
        prompt.authenticate(
            BiometricPrompt.CryptoObject(cipher),
            cancellation,
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(authentication: BiometricPrompt.AuthenticationResult) {
                    finish(authentication.cryptoObject?.cipher)
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    finish(null)
                }
            },
        )
    }
