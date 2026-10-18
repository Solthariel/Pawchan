package com.solthariel.pawchan.domain.filter

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/** Work budget prevents a pathological user expression from monopolizing a worker. */
internal class BoundedFilterRegex(
    expression: String,
) {
    private val pattern =
        try {
            Pattern.compile(expression, Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.DOTALL)
        } catch (_: PatternSyntaxException) {
            null
        } catch (_: StackOverflowError) {
            null
        }

    fun find(text: String): IntRange? {
        val compiled = pattern ?: return null
        return try {
            val matcher = compiled.matcher(RegexInput(text, RegexBudget()))
            if (matcher.find() && matcher.end() > matcher.start()) matcher.start() until matcher.end() else null
        } catch (_: RegexBudgetExceeded) {
            null
        } catch (_: StackOverflowError) {
            null
        }
    }
}

internal class RegexBudget {
    private var remaining = FILTER_REGEX_READ_BUDGET

    fun consume() {
        if (--remaining < 0) throw RegexBudgetExceeded()
    }
}

internal class RegexBudgetExceeded : RuntimeException()

internal class RegexInput(
    private val text: String,
    private val budget: RegexBudget,
) : CharSequence {
    override val length: Int get() = text.length

    override fun get(index: Int): Char {
        budget.consume()
        return text[index]
    }

    override fun subSequence(
        startIndex: Int,
        endIndex: Int,
    ): CharSequence = RegexInput(text.substring(startIndex, endIndex), budget)

    override fun toString(): String = text
}

fun contentFilterValidationError(rule: ContentFilterRule): String? =
    when {
        !rule.threads && !rule.comments -> {
            "Выберите ОП-пост или ответы"
        }

        !rule.targets.catalog && !rule.targets.thread -> {
            "Выберите каталог или открытый тред"
        }

        rule.mode == ContentFilterMode.EXACT_PHRASE && rule.values.size != 1 -> {
            "Введите одну точную фразу"
        }

        rule.values.isEmpty() || rule.values.any { it.isBlank() } -> {
            "Условие пустое"
        }

        rule.options.maxGap != null && rule.options.maxGap < 0 -> {
            "Расстояние должно быть неотрицательным"
        }

        rule.mode != ContentFilterMode.REGEX -> {
            null
        }

        rule.values.size != 1 -> {
            "Введите одно регулярное выражение"
        }

        rule.values.single().length > MAX_FILTER_REGEX_LENGTH -> {
            "Regex длиннее $MAX_FILTER_REGEX_LENGTH символов"
        }

        else -> {
            regexValidationError(rule.values.single())
        }
    }

private fun regexValidationError(expression: String): String? =
    try {
        Pattern.compile(expression)
        null
    } catch (error: PatternSyntaxException) {
        "${error.description}, позиция ${error.index}"
    } catch (_: StackOverflowError) {
        "Слишком глубокая вложенность regex"
    }

private const val FILTER_REGEX_READ_BUDGET = 250_000
private const val MAX_FILTER_REGEX_LENGTH = 4096
