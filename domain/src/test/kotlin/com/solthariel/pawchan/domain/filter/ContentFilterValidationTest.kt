package com.solthariel.pawchan.domain.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterValidationTest {
    private val rule =
        ContentFilterRule(
            "rule",
            "2ch",
            condition = ContentFilterConditionSpec(values = listOf("phrase")),
        )

    @Test
    fun validationPreservesTargetEmptyAndGapErrors() {
        assertEquals(
            "Выберите ОП-пост или ответы",
            contentFilterValidationError(rule.copy(targets = ContentFilterTargets(threads = false))),
        )
        assertEquals(
            "Условие пустое",
            contentFilterValidationError(rule.copy(condition = rule.condition.copy(values = emptyList()))),
        )
        assertEquals(
            "Расстояние должно быть неотрицательным",
            contentFilterValidationError(rule.copy(options = ContentFilterOptions(maxGap = -1))),
        )
        assertNull(contentFilterValidationError(rule))
    }

    @Test
    fun postTargetsAndDisplayLocationsAreValidatedIndependently() {
        val targets =
            listOf(
                ContentFilterTargets(threads = true, comments = false),
                ContentFilterTargets(threads = false, comments = true),
                ContentFilterTargets(threads = true, comments = true),
            )
        targets.forEach { target ->
            assertNull(contentFilterValidationError(rule.copy(targets = target)))
            assertNull(contentFilterValidationError(rule.copy(targets = target.copy(catalog = true, thread = false))))
            assertNull(contentFilterValidationError(rule.copy(targets = target.copy(catalog = false, thread = true))))
            assertEquals(
                "Выберите каталог или открытый тред",
                contentFilterValidationError(rule.copy(targets = target.copy(catalog = false, thread = false))),
            )
        }
    }

    @Test
    fun emptyAndBlankConditionsRetainTheirModeSpecificErrors() {
        ContentFilterMode.entries.forEach { mode ->
            val condition = rule.condition.copy(mode = mode, values = emptyList())
            val expected = if (mode == ContentFilterMode.EXACT_PHRASE) "Введите одну точную фразу" else "Условие пустое"
            assertEquals(expected, contentFilterValidationError(rule.copy(condition = condition)))
            assertEquals(
                "Условие пустое",
                contentFilterValidationError(rule.copy(condition = condition.copy(values = listOf(" \t")))),
            )
        }
    }

    @Test
    fun exactPhraseRequiresOneValueWithoutChangingOtherModes() {
        val one = rule.condition.copy(mode = ContentFilterMode.EXACT_PHRASE, values = listOf("голова дай хлеб"))
        assertNull(contentFilterValidationError(rule.copy(condition = one)))
        val multiple = one.copy(values = listOf("голова", "хлеб"))
        assertEquals("Введите одну точную фразу", contentFilterValidationError(rule.copy(condition = multiple)))
        assertNull(
            contentFilterValidationError(rule.copy(condition = multiple.copy(mode = ContentFilterMode.ALL_WORDS))),
        )
    }

    @Test
    fun regexLimitAndSingleExpressionRequirementRemainExplicit() {
        val oversized = rule.copy(condition = rule.condition.copy(values = listOf("a".repeat(OVERSIZED_REGEX_LENGTH))))
        assertNull(contentFilterValidationError(oversized))
        val regex = oversized.copy(condition = oversized.condition.copy(mode = ContentFilterMode.REGEX))
        assertEquals("Regex длиннее 4096 символов", contentFilterValidationError(regex))
        assertEquals(
            "Введите одно регулярное выражение",
            contentFilterValidationError(regex.copy(condition = regex.condition.copy(values = listOf("a", "b")))),
        )
    }
}

private const val OVERSIZED_REGEX_LENGTH = 4097
