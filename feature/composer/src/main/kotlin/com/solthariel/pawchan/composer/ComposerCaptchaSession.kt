package com.solthariel.pawchan.composer

import android.os.SystemClock
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.withAccountLimits
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class ComposerCaptchaContext(
    val board: BoardAddress,
    val thread: ThreadId? = null,
)

internal data class ComposerCaptchaSnapshot(
    val captcha: ComposerCaptchaState,
    val progress: ComposerProgressState,
    val failure: ComposerFailureState,
    val rules: PostingRules?,
)

internal data class ComposerCaptchaBindings(
    val read: () -> ComposerCaptchaSnapshot,
    val write: (ComposerCaptchaSnapshot) -> Unit,
    val fail: (PostingOutcome.Failure) -> Unit,
)

/** Owned by the form scope; submit/draft/navigation are deliberately not part of this session. */
internal class ComposerCaptchaSession(
    private val scope: CoroutineScope,
    private val posting: PostingRepository,
    private val context: ComposerCaptchaContext,
    private val bindings: ComposerCaptchaBindings,
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime,
    private val preparationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private var job: Job? = null

    fun cancel() {
        job?.cancel()
        job = null
    }

    fun load(failure: ComposerFailureState = ComposerFailureState()) {
        cancel()
        bindings.write(
            bindings.read().copy(
                captcha = ComposerCaptchaState(),
                progress = ComposerProgressState(captchaLoading = true),
                failure = failure,
            ),
        )
        job = scope.launch { loadChallenge() }
    }

    fun select(number: Int) {
        val current = bindings.read()
        if (current.progress.sent || current.progress.submitting || current.progress.captchaSelecting) return
        val challenge = current.captcha.challenge
        val selected = challenge?.options?.firstOrNull { it.number == number }
        if (challenge == null || selected == null) return
        if (current.captcha.deadlineElapsedMillis != null && current.captcha.isExpired(nowMillis())) {
            bindings.fail(PostingOutcome.Failure(com.solthariel.pawchan.domain.posting.PostingFailure.CaptchaRejected))
        } else {
            selectValidChallenge(current, challenge, selected.image, number)
        }
    }

    private fun selectValidChallenge(
        current: ComposerCaptchaSnapshot,
        challenge: EmojiCaptchaChallenge,
        image: ByteArray,
        number: Int,
    ) {
        cancel()
        bindings.write(
            current.copy(progress = current.progress.copy(captchaSelecting = true), failure = ComposerFailureState()),
        )
        job =
            scope.launch {
                when (val outcome = posting.selectEmoji(challenge.id, number)) {
                    is PostingOutcome.Success -> {
                        currentCoroutineContext().ensureActive()
                        val state = bindings.read()
                        val captcha =
                            withContext(preparationDispatcher) { state.captcha.acceptStep(outcome.value, image) }
                        currentCoroutineContext().ensureActive()
                        val latest = bindings.read()
                        bindings.write(
                            latest.copy(
                                captcha = captcha,
                                progress = latest.progress.copy(captchaSelecting = false),
                            ),
                        )
                    }

                    is PostingOutcome.Failure -> {
                        currentCoroutineContext().ensureActive()
                        bindings.fail(outcome)
                    }
                }
            }
    }

    private suspend fun loadChallenge() {
        val rules =
            when (val outcome = posting.loadRules(context.board)) {
                is PostingOutcome.Success -> {
                    outcome.value
                }

                is PostingOutcome.Failure -> {
                    currentCoroutineContext().ensureActive()
                    bindings.fail(outcome)
                    return
                }
            }
        currentCoroutineContext().ensureActive()
        bindings.write(bindings.read().copy(rules = rules))
        when (val outcome = posting.loadCaptcha(context.board, context.thread)) {
            is PostingOutcome.Success -> {
                currentCoroutineContext().ensureActive()
                bindings.write(
                    bindings.read().copy(
                        rules = rules.withAccountLimits(outcome.value.uploadLimits),
                        captcha =
                            ComposerCaptchaState(
                                challenge = outcome.value.challenge,
                                satisfied = outcome.value.challenge == null,
                                deadlineElapsedMillis =
                                    outcome.value.challenge?.lifetimeSeconds?.let {
                                        nowMillis() + it.toLong() * CAPTCHA_MILLIS_PER_SECOND
                                    },
                            ),
                        progress = ComposerProgressState(),
                    ),
                )
            }

            is PostingOutcome.Failure -> {
                currentCoroutineContext().ensureActive()
                bindings.fail(outcome)
            }
        }
    }
}

private fun ComposerCaptchaState.acceptStep(
    step: EmojiCaptchaStep,
    image: ByteArray,
): ComposerCaptchaState {
    val selected = selectedImages + listOf(image)
    return when (step) {
        is EmojiCaptchaStep.Challenge -> {
            ComposerCaptchaState(
                challenge =
                    EmojiCaptchaChallenge(
                        requireNotNull(challenge).id,
                        challenge.taskImage.takeIf { it.contentEquals(step.taskImage) } ?: step.taskImage,
                        step.options,
                        challenge.lifetimeSeconds,
                    ),
                keyboard = stableCaptchaKeyboard(keyboard, step.options),
                selectedImages = selected,
                deadlineElapsedMillis = deadlineElapsedMillis,
            )
        }

        is EmojiCaptchaStep.Solved -> {
            ComposerCaptchaState(selectedImages = selected, token = step.token, satisfied = true)
        }
    }
}

private const val CAPTCHA_MILLIS_PER_SECOND = 1_000L
