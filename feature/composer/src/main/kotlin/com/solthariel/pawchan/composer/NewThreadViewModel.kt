package com.solthariel.pawchan.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewThreadUiState(
    val draft: ComposerDraftState = ComposerDraftState(),
    val captchaState: ComposerCaptchaState = ComposerCaptchaState(),
    val progress: ComposerProgressState = ComposerProgressState(captchaLoading = true),
    val failure: ComposerFailureState = ComposerFailureState(),
    val createdThread: ThreadAddress? = null,
    val rules: PostingRules? = null,
) {
    val subject: String
        get() = draft.subject
    val comment: String
        get() = draft.comment
    val attachments: List<ComposerAttachment>
        get() = draft.attachments
    val captcha: EmojiCaptchaChallenge?
        get() = captchaState.challenge
    val selectedEmojiImages: List<ByteArray>
        get() = captchaState.selectedImages
    val emojiCaptchaToken: String?
        get() = captchaState.token
    val captchaSatisfied: Boolean
        get() = captchaState.satisfied
    val captchaLoading: Boolean
        get() = progress.captchaLoading
    val captchaSelecting: Boolean
        get() = progress.captchaSelecting
    val submitting: Boolean
        get() = progress.submitting
    val error: ReplyError?
        get() = failure.error
    val rejectionReason: String?
        get() = failure.reason
    val canSubmit: Boolean
        get() =
            subject.length <= NewThreadDraft.MAX_SUBJECT_LENGTH &&
                draft.tag.length <= NewThreadDraft.MAX_TAG_LENGTH &&
                comment.isNotBlank() &&
                comment.length <= (rules?.maxCommentLength ?: ReplyDraft.MAX_COMMENT_LENGTH) &&
                attachments.isNotEmpty() &&
                preparedSelectionAllowed(attachments, rules) &&
                !draft.filesLoading &&
                attachments.all { it.validName } &&
                captchaSatisfied &&
                !captchaLoading &&
                !captchaSelecting &&
                !submitting &&
                !progress.sent &&
                error == null
}

class NewThreadViewModel internal constructor(
    private val board: BoardAddress,
    private val posting: PostingRepository,
    private val prepareFiles: AttachmentPreparer = ::prepareComposerAttachments,
    drafts: ComposerDraftCoordinator? = null,
    autoOpen: Boolean = true,
    preparationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NewThreadUiState())
    private var job: Job? = null
    private val rateLimit = ComposerRateLimit()
    private val submission = ComposerSubmissionGuard()
    private var active = false
    private var openedBefore = false

    private val uploadPreparation =
        ComposerUploadPreparation(
            viewModelScope,
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
            viewModelScope,
            drafts,
            ComposerDraftKey(board),
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
            viewModelScope,
            posting,
            ComposerCaptchaContext(board),
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

    val state: StateFlow<NewThreadUiState> = mutableState.asStateFlow()

    init {
        if (autoOpen) open()
    }

    fun open() {
        if (active) return
        active = true
        val reset = mutableState.value.draft.resetId + if (openedBefore) 1 else 0
        openedBefore = true
        mutableState.value = NewThreadUiState(draft = ComposerDraftState(resetId = reset))
        draftSession.restore()
        val pending = submission.activeFailure()
        if (pending != null) {
            mutableState.value = mutableState.value.copy(progress = ComposerProgressState(), failure = pending)
        } else {
            reloadCaptcha()
        }
    }

    fun close(onClosed: () -> Unit) {
        val current = mutableState.value
        val busy = current.submitting || (current.draft.filesLoading && !draftSession.restoring)
        if (!active || busy) return
        job?.cancel()
        captchaSession.cancel()
        uploadPreparation.update(emptyList())
        draftSession.close(mutableState.value.draft) {
            mutableState.value =
                NewThreadUiState(draft = ComposerDraftState(resetId = mutableState.value.draft.resetId + 1))
            active = false
            onClosed()
        }
    }

    fun saveDraftNow() {
        if (active) draftSession.saveNow(mutableState.value.draft)
    }

    fun updateDraft(draft: ComposerDraftState) {
        val current = mutableState.value
        val editable = !current.submitting && !current.progress.sent
        if (active && editable && draft.resetId == current.draft.resetId) {
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
        if (!current.submitting && !current.progress.sent) {
            mutableState.value =
                current.copy(
                    draft = ComposerDraftState(resetId = current.draft.resetId + 1),
                    failure = rateLimit.activeFailure() ?: submission.activeFailure() ?: ComposerFailureState(),
                )
            uploadPreparation.update(emptyList())
            draftSession.clear(mutableState.value.draft)
        }
    }

    var lastCreatedPost: com.solthariel.pawchan.domain.model.BoardPost? = null
        private set

    fun reloadCaptcha() {
        rateLimit.activeFailure()?.let {
            mutableState.value = mutableState.value.copy(progress = ComposerProgressState(), failure = it)
            return
        }
        if (mutableState.value.progress.sent || mutableState.value.submitting) return
        submission.acknowledge()
        job?.cancel()
        captchaSession.load()
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
            viewModelScope.launch {
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
                    NewThreadDraft(
                        subject = if (rules.subjectEnabled) current.subject else "",
                        comment = current.comment,
                        emojiCaptchaToken = current.emojiCaptchaToken,
                        attachments = attachments,
                        email = current.draft.email,
                        options = current.draft.options.allowedBy(rules),
                    )
                when (
                    val outcome =
                        posting.createThread(
                            board,
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
        current: NewThreadUiState,
        draft: NewThreadDraft,
        value: com.solthariel.pawchan.domain.model.ThreadAddress,
    ) {
        lastCreatedPost = postedNewThread(value, draft)
        active = false
        uploadPreparation.update(emptyList())
        draftSession.sent()
        mutableState.value =
            current.copy(
                progress = ComposerProgressState(sent = true),
                failure = ComposerFailureState(),
            )
        delay(COMPOSER_SUCCESS_DISPLAY_MILLIS)
        mutableState.value = mutableState.value.copy(createdThread = value)
    }

    private fun handleFailure(outcome: PostingOutcome.Failure) {
        submission.record(outcome.reason)
        mutableState.value =
            mutableState.value.copy(
                progress = ComposerProgressState(),
                failure = submission.activeFailure() ?: rateLimit.failure(outcome.reason),
            )
    }

    companion object {
        fun factory(
            board: BoardAddress,
            posting: PostingRepository,
            drafts: ComposerDraftCoordinator? = null,
            autoOpen: Boolean = true,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { NewThreadViewModel(board, posting, drafts = drafts, autoOpen = autoOpen) }
            }
    }
}
