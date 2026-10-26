package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReplyComposerController internal constructor(
    private val scope: CoroutineScope,
    private val address: ThreadAddress,
    private val posting: PostingRepository,
    private val onPosted: (BoardPost) -> Unit,
    private val prepareFiles: AttachmentPreparer = ::prepareComposerAttachments,
    drafts: ComposerDraftCoordinator? = null,
    preparationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val mutableState = MutableStateFlow(ReplyComposerState())
    private var job: Job? = null
    private val rateLimit = ComposerRateLimit()
    private val submission = ComposerSubmissionGuard()
    private val persistent = drafts != null

    private val uploadPreparation =
        ComposerUploadPreparation(
            scope,
            preparationDispatcher,
        ) { prepared ->
            mutableState.update { current ->
                if (sameUploadSelection(current.draft.attachments, prepared)) {
                    current.copy(draft = current.draft.copy(attachments = prepared))
                } else {
                    current
                }
            }
        }

    private val draftSession =
        ComposerDraftSession(
            scope,
            drafts,
            ComposerDraftKey(address.board, address.thread),
            ComposerDraftSessionActions(
                read = { mutableState.value.draft },
                write = {
                    mutableState.value = mutableState.value.copy(draft = it)
                    uploadPreparation.update(it.attachments)
                },
                fail = {
                    mutableState.value =
                        mutableState.value.copy(
                            failure = ComposerFailureState(ReplyError.REJECTED, it),
                        )
                },
            ),
        )

    private val captchaSession =
        ComposerCaptchaSession(
            scope,
            posting,
            ComposerCaptchaContext(address.board, address.thread),
            ComposerCaptchaBindings(
                read = {
                    val current = mutableState.value
                    ComposerCaptchaSnapshot(current.captchaState, current.progress, current.failure, current.rules)
                },
                write = { update ->
                    mutableState.value =
                        mutableState.value.copy(
                            captchaState = update.captcha,
                            progress = update.progress,
                            failure = update.failure,
                            rules = update.rules,
                        )
                },
                fail = ::handleFailure,
            ),
            preparationDispatcher = preparationDispatcher,
        )

    val state: StateFlow<ReplyComposerState> = mutableState.asStateFlow()

    fun open(
        threadClosed: Boolean,
        insertion: String? = null,
    ) {
        if (threadClosed) return
        if (!mutableState.value.visible) {
            mutableState.value =
                ReplyComposerState(
                    draft = ComposerDraftState(resetId = mutableState.value.draft.resetId),
                    visible = true,
                    progress = ComposerProgressState(captchaLoading = true),
                )
            draftSession.restore()
            val pending = submission.activeFailure()
            if (pending != null) {
                mutableState.value = mutableState.value.copy(progress = ComposerProgressState(), failure = pending)
            } else {
                reloadCaptcha()
            }
        }
        if (insertion != null) {
            val resetId = mutableState.value.draft.resetId
            scope.launch {
                draftSession.awaitRestore()
                val current = mutableState.value
                val unchangedForm = current.visible && current.draft.resetId == resetId
                val editable = !current.submitting && !current.progress.sent && !current.draft.filesLoading
                if (unchangedForm && editable) {
                    val draft =
                        appendReplyInsertion(
                            current.draft,
                            insertion,
                            current.rules?.maxCommentLength ?: ReplyComposerState.MAX_COMMENT_LENGTH,
                        )
                    if (draft == null) {
                        mutableState.value =
                            current.copy(
                                failure =
                                    ComposerFailureState(
                                        ReplyError.REJECTED,
                                        "Цитата превышает предел длины комментария",
                                    ),
                            )
                    } else {
                        updateDraft(draft)
                    }
                }
            }
        }
    }

    fun close() {
        val current = mutableState.value
        if (!current.visible) return
        val busy = current.submitting || (current.draft.filesLoading && !draftSession.restoring)
        if (persistent && busy) return
        job?.cancel()
        captchaSession.cancel()
        uploadPreparation.update(emptyList())
        draftSession.close(current.draft) {
            mutableState.value = ReplyComposerState(draft = ComposerDraftState(resetId = current.draft.resetId + 1))
        }
    }

    fun saveDraftNow() {
        if (mutableState.value.visible && !mutableState.value.progress.sent) {
            draftSession.saveNow(mutableState.value.draft)
        }
    }

    fun updateDraft(draft: ComposerDraftState) {
        val current = mutableState.value
        val editable = !current.submitting && !current.progress.sent
        if (current.visible && editable && draft.resetId == current.draft.resetId) {
            mutableState.value =
                current.copy(
                    draft = draft.copy(attachments = draft.attachments.toList()),
                    failure =
                        rateLimit.activeFailure()
                            ?: submission.activeFailure()
                            ?: if (current.rules == null) current.failure else ComposerFailureState(),
                )
            uploadPreparation.update(mutableState.value.draft.attachments)
            draftSession.changed(mutableState.value.draft)
        }
    }

    fun clearAll() {
        val current = mutableState.value
        if (current.visible && !current.submitting && !current.progress.sent) {
            mutableState.value =
                current.copy(
                    draft = ComposerDraftState(resetId = current.draft.resetId + 1),
                    failure = rateLimit.activeFailure() ?: submission.activeFailure() ?: ComposerFailureState(),
                )
            uploadPreparation.update(emptyList())
            draftSession.clear(mutableState.value.draft)
        }
    }

    fun reloadCaptcha() {
        rateLimit.activeFailure()?.let {
            mutableState.value = mutableState.value.copy(progress = ComposerProgressState(), failure = it)
            return
        }
        if (mutableState.value.progress.sent || mutableState.value.submitting) return
        submission.acknowledge()
        loadCaptcha(error = null, reason = null)
    }

    fun selectEmoji(number: Int) {
        if (rateLimit.activeFailure() != null || submission.activeFailure() != null) return
        val current = mutableState.value
        if (current.progress.sent || current.submitting) return
        job?.cancel()
        captchaSession.select(number)
    }

    fun submit(hidden: Set<com.solthariel.pawchan.domain.settings.ComposerField> = emptySet()) {
        if (rateLimit.activeFailure() != null || submission.activeFailure() != null) return
        val current = mutableState.value.let { it.copy(draft = it.draft.withVisibleFields(hidden)) }
        val rules = current.rules
        if (!current.canSubmit || rules == null) {
            return
        }
        job?.cancel()
        captchaSession.cancel()
        mutableState.value =
            current.copy(
                progress = current.progress.copy(submitting = true, upload = null),
                failure = ComposerFailureState(),
            )
        job =
            scope.launch {
                val attachments =
                    when (val result = prepareFiles(current.attachments, rules)) {
                        is PostingOutcome.Success -> {
                            result.value
                        }

                        is PostingOutcome.Failure -> {
                            handleFailure(result)
                            return@launch
                        }
                    }
                val draft =
                    ReplyDraft(
                        comment = current.comment,
                        emojiCaptchaToken = current.emojiCaptchaToken,
                        attachments = attachments,
                        email = current.draft.email,
                        options = current.draft.options.allowedBy(rules),
                    )
                when (
                    val outcome =
                        posting.submitReply(
                            address,
                            draft,
                            composerProgressObserver(
                                coroutineContext[kotlinx.coroutines.Job],
                                mutableState,
                            ) { state, upload ->
                                state.copy(progress = state.progress.copy(upload = upload))
                            },
                        )
                ) {
                    is PostingOutcome.Success -> {
                        completeSubmission(current, draft, outcome.value)
                    }

                    is PostingOutcome.Failure -> {
                        handleFailure(outcome)
                    }
                }
            }
    }

    private suspend fun completeSubmission(
        current: ReplyComposerState,
        draft: ReplyDraft,
        value: com.solthariel.pawchan.domain.model.PostId,
    ) {
        val posted = postedBoardPost(address, value, draft)
        uploadPreparation.update(emptyList())
        draftSession.sent()
        mutableState.value =
            current.copy(
                progress = ComposerProgressState(sent = true),
                failure = ComposerFailureState(),
            )
        onPosted(posted)
        delay(COMPOSER_SUCCESS_DISPLAY_MILLIS)
        mutableState.value =
            ReplyComposerState(draft = ComposerDraftState(resetId = current.draft.resetId + 1))
    }

    private fun loadCaptcha(
        error: ReplyError?,
        reason: String?,
    ) {
        if (!mutableState.value.visible) return
        job?.cancel()
        captchaSession.load(ComposerFailureState(error, reason))
    }

    private fun handleFailure(outcome: PostingOutcome.Failure) {
        submission.record(outcome.reason)
        val (error, reason) = outcome.reason.toReplyError()
        mutableState.value =
            mutableState.value.copy(
                progress = ComposerProgressState(),
                failure = submission.activeFailure() ?: rateLimit.failure(outcome.reason),
            )
        if (error == ReplyError.CAPTCHA) {
            loadCaptcha(error, reason)
        }
    }
}
