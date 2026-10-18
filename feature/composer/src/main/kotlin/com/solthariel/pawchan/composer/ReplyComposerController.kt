package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReplyComposerController internal constructor(
    private val scope: CoroutineScope,
    private val address: ThreadAddress,
    private val posting: PostingRepository,
    private val onPosted: (BoardPost) -> Unit,
    private val prepareFiles: AttachmentPreparer = ::prepareComposerAttachments,
    drafts: ComposerDraftCoordinator? = null,
) {
    private val mutableState = MutableStateFlow(ReplyComposerState())
    private var job: Job? = null
    private val persistent = drafts != null

    private val draftSession =
        ComposerDraftSession(
            scope,
            drafts,
            ComposerDraftKey(address.board, address.thread),
            ComposerDraftSessionActions(
                read = { mutableState.value.draft },
                write = { mutableState.value = mutableState.value.copy(draft = it) },
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
            reloadCaptcha()
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
                    failure = if (current.rules == null) current.failure else ComposerFailureState(),
                )
            draftSession.changed(mutableState.value.draft)
        }
    }

    fun clearAll() {
        val current = mutableState.value
        if (current.visible && !current.submitting && !current.progress.sent) {
            mutableState.value = current.copy(draft = ComposerDraftState(resetId = current.draft.resetId + 1))
            draftSession.clear(mutableState.value.draft)
        }
    }

    fun reloadCaptcha() {
        if (mutableState.value.progress.sent || mutableState.value.submitting) return
        loadCaptcha(error = null, reason = null)
    }

    fun selectEmoji(number: Int) {
        val current = mutableState.value
        if (current.progress.sent || current.submitting) return
        job?.cancel()
        captchaSession.select(number)
    }

    fun submit(hidden: Set<com.solthariel.pawchan.domain.settings.ComposerField> = emptySet()) {
        val current = mutableState.value.let { it.copy(draft = it.draft.withVisibleFields(hidden)) }
        val rules = current.rules
        if (!current.canSubmit || rules == null) {
            return
        }
        job?.cancel()
        captchaSession.cancel()
        mutableState.value =
            current.copy(
                progress = current.progress.copy(submitting = true),
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
                when (val outcome = posting.submitReply(address, draft)) {
                    is PostingOutcome.Success -> {
                        val posted = postedBoardPost(address, outcome.value, draft)
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

                    is PostingOutcome.Failure -> {
                        handleFailure(outcome)
                    }
                }
            }
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
        val (error, reason) = outcome.reason.toReplyError()
        mutableState.value =
            mutableState.value.copy(
                progress = ComposerProgressState(),
                failure = ComposerFailureState(error, reason),
            )
        if (error == ReplyError.CAPTCHA) {
            loadCaptcha(error, reason)
        }
    }
}
