package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.PostingRules

internal data class ComposerUploadBudget(
    val remaining: Long?,
    val problem: String? = null,
) {
    val ready: Boolean get() = remaining != null && remaining >= 0 && problem == null
    val status: String? get() =
        when {
            problem != null -> problem
            remaining == null -> "Расчёт размера…"
            remaining < 0 -> "Превышен общий размер вложений"
            else -> null
        }
}

internal fun composerUploadBudget(
    files: List<ComposerAttachment>,
    rules: PostingRules?,
): ComposerUploadBudget {
    val failure =
        if (files.any { !it.validName }) {
            "Недопустимое имя файла"
        } else {
            files.firstNotNullOfOrNull { file ->
                file.prepared?.takeIf { it.source === file.source && it.options == file.options }?.failure
            }
        }
    return when {
        failure != null -> {
            ComposerUploadBudget(null, failure)
        }

        files.any { it.preparedUpload == null } -> {
            ComposerUploadBudget(null)
        }

        else -> {
            val used = files.sumOf { requireNotNull(it.preparedUpload).content.size.toLong() }
            val limit =
                minOf((rules?.maxTotalFileBytes ?: DEFAULT_UPLOAD_BUDGET).toLong(), PostingRules.MAX_TOTAL_BYTES)
            ComposerUploadBudget(limit - used)
        }
    }
}

internal fun preparedSelectionAllowed(
    files: List<ComposerAttachment>,
    rules: PostingRules?,
): Boolean =
    rules != null && composerUploadBudget(files, rules).ready &&
        rules.acceptsSelection(files.map { requireNotNull(it.preparedUpload) })

internal fun remainingUploadMegabytes(bytes: Long): String {
    if (bytes < 0 && bytes > -MIN_TENTH_MEGABYTE) return "−<0,1"
    val value = formatComposerMegabytes(kotlin.math.abs(bytes).toInt())
    return if (bytes < 0) "−$value" else value
}

private const val DEFAULT_UPLOAD_BUDGET = 40 * 1024 * 1024
private const val MIN_TENTH_MEGABYTE = 1024 * 1024 / 10
