package com.solthariel.pawchan.domain.filter

fun contentFilterValidationError(rule: ContentFilterRule): String? = contentFilterRecordValidationError(rule)

/** No migration and no execution of previous phrase/regex records. */
fun contentFilterRecordValidationError(rule: ContentFilterRule): String? =
    when {
        !rule.mode.isBuilderMode -> {
            "Старый формат правил больше не поддерживается"
        }

        oversizedFilterCondition(rule) -> {
            "До $MAX_FILTER_VALUES значений/исключений и " +
                "$MAX_FILTER_CONDITION_CHARACTERS символов в условии"
        }

        !rule.threads || !rule.comments || rule.fields != ContentFilterFields.EITHER ||
            !rule.targets.catalog || !rule.targets.thread -> {
            "Правило должно применяться ко всем постам и обоим полям"
        }

        rule.mode == ContentFilterMode.PHRASE && rule.values.size != 1 -> {
            "Введите одну фразу"
        }

        rule.values.isEmpty() || rule.values.any { it.isBlank() } -> {
            "Условие пустое"
        }

        else -> {
            null
        }
    }
