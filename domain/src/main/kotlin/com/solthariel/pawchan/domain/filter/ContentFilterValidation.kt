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

        (!rule.threads && !rule.comments) || rule.fields != ContentFilterFields.EITHER ||
            !rule.targets.catalog || !rule.targets.thread -> {
            "Выберите ОП-посты, комментарии или все посты; проверяются заголовок и текст"
        }

        rule.options.textMatch.boundary == FilterBoundary.PREFIX -> {
            "Выберите совпадение слова целиком или по части"
        }

        rule.mode == ContentFilterMode.PHRASE && rule.values.size != 1 -> {
            "Введите одну фразу"
        }

        rule.values.isEmpty() || rule.conditionGroups().any { it.isEmpty() || it.any(String::isBlank) } -> {
            "Условие пустое"
        }

        else -> {
            null
        }
    }
