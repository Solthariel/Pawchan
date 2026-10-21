package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal object PostingRequestFactory {
    fun boardRules(board: BoardAddress): Request = jsonGet("$DVACH_ORIGIN/${board.board.value}/index.json")

    fun captchaId(
        board: BoardAddress,
        thread: ThreadId?,
    ): Request {
        val builder =
            "$DVACH_ORIGIN/api/captcha/emoji/id"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("board", board.board.value)
        thread?.let { builder.addQueryParameter("thread", it.value.toString()) }
        return jsonGet(builder.build().toString())
    }

    fun captchaShow(id: String): Request {
        val url =
            "$DVACH_ORIGIN/api/captcha/emoji/show"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("id", id)
                .build()
        return jsonGet(url.toString())
    }

    fun captchaClick(
        id: String,
        emojiNumber: Int,
    ): Request {
        val json =
            buildJsonObject {
                put("captchaTokenId", id)
                put("emojiNumber", emojiNumber)
            }.toString()
        return Request
            .Builder()
            .url("$DVACH_ORIGIN/api/captcha/emoji/click")
            .header("Accept", "application/json")
            .post(json.toRequestBody(JSON_REQUEST_MEDIA_TYPE))
            .build()
    }

    fun reply(
        address: ThreadAddress,
        draft: ReplyDraft,
        observer: PostingProgressObserver = PostingProgressObserver.None,
    ): Request =
        post(
            PostForm(
                board = address.board,
                thread = address.thread,
                subject = "",
                comment = draft.comment,
                captchaToken = draft.emojiCaptchaToken,
                attachments = draft.attachments,
                author = PostingAuthor(draft.email, draft.options),
            ),
            observer,
        )

    fun newThread(
        board: BoardAddress,
        draft: NewThreadDraft,
        observer: PostingProgressObserver = PostingProgressObserver.None,
    ): Request =
        post(
            PostForm(
                board = board,
                thread = null,
                subject = draft.subject,
                comment = draft.comment,
                captchaToken = draft.emojiCaptchaToken,
                attachments = draft.attachments,
                author = PostingAuthor(draft.email, draft.options),
            ),
            observer,
        )

    private fun post(
        form: PostForm,
        observer: PostingProgressObserver,
    ): Request {
        val tracker = PostingUploadTracker(form.attachments.map { it.content.size.toLong() }, observer)
        val body =
            MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("task", "post")
                .addFormDataPart("board", form.board.board.value)
                .addFormDataPart("thread", form.thread?.value?.toString() ?: "0")
                .addFormDataPart("email", if (form.author.options.sage) "sage" else form.author.email)
                .addFormDataPart("name", form.author.options.name)
                .addFormDataPart("subject", form.subject)
                .addFormDataPart("tags", form.author.options.tag)
                .addFormDataPart("comment", form.comment)
                .apply {
                    form.author.options.icon
                        ?.let { addFormDataPart("icon", it.toString()) }
                    if (form.author.options.threadAuthor) addFormDataPart("op_mark", "1")
                    addAttachments(form.attachments, tracker, observer)
                    form.captchaToken?.let { token ->
                        addFormDataPart("captcha_type", "emoji_captcha")
                        addFormDataPart("emoji_captcha_id", token)
                    }
                }.build()
        return Request
            .Builder()
            .url("$DVACH_ORIGIN/user/posting")
            .header("Accept", "application/json")
            .post(if (observer === PostingProgressObserver.None) body else tracker.request(body))
            .build()
    }

    private fun MultipartBody.Builder.addAttachments(
        files: List<ReplyAttachment>,
        tracker: PostingUploadTracker,
        observer: PostingProgressObserver,
    ) {
        files.forEachIndexed { index, file ->
            addFormDataPart(
                "file[]",
                file.fileName,
                if (observer === PostingProgressObserver.None) {
                    file.content.toRequestBody(file.mimeType?.toMediaTypeOrNull())
                } else {
                    tracker.file(index, file.content, file.mimeType?.toMediaTypeOrNull())
                },
            )
            if (file.spoiler) addFormDataPart("file_${index}_nsfw", "1")
        }
    }

    private fun jsonGet(url: String): Request =
        Request
            .Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
}

private data class PostForm(
    val board: BoardAddress,
    val thread: ThreadId?,
    val subject: String,
    val comment: String,
    val captchaToken: String?,
    val attachments: List<ReplyAttachment>,
    val author: PostingAuthor,
)

private val JSON_REQUEST_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

private data class PostingAuthor(
    val email: String,
    val options: PostingOptions,
)
