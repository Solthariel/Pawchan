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

/** Execution validation: retired modes cannot hide posts, even after a successful restore. */
fun contentFilterValidationError(rule: ContentFilterRule): String? =
    if (!rule.mode.isCurrentFilterMode) {
        "Старое правило не применяется. Выберите точную фразу или регулярное выражение и сохраните"
    } else {
        contentFilterRecordValidationError(rule)
    }

/** Storage validation preserves bounded legacy records without converting or executing their conditions. */
fun contentFilterRecordValidationError(rule: ContentFilterRule): String? =
    when {
        oversizedFilterCondition(rule) -> {
            "До $MAX_FILTER_VALUES значений/исключений и $MAX_FILTER_CONDITION_CHARACTERS символов в условии"
        }

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

        else -> {
            regexConditionError(rule)
        }
    }

private fun regexConditionError(rule: ContentFilterRule): String? =
    when {
        rule.values.size != 1 -> "Введите одно регулярное выражение"
        rule.values.single().length > MAX_FILTER_REGEX_LENGTH -> "Regex длиннее $MAX_FILTER_REGEX_LENGTH символов"
        else -> regexValidationError(rule.values.single())
    }

private fun regexValidationError(expression: String): String? =
    try {
        Pattern.compile(expression, Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.DOTALL)
        null
    } catch (error: PatternSyntaxException) {
        val position = if (error.index >= 0) ", позиция ${error.index + 1}" else ""
        if (error.description.contains("flag", ignoreCase = true)) {
            "Недопустимый флаг регулярного выражения$position. Android не поддерживает (?U)"
        } else {
            "Ошибка в регулярном выражении$position. Проверьте скобки, повторы и экранирование"
        }
    } catch (_: StackOverflowError) {
        "Слишком глубокая вложенность regex"
    }

private const val FILTER_REGEX_READ_BUDGET = 250_000
private const val MAX_FILTER_REGEX_LENGTH = 4096
