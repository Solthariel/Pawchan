package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.postpreferences.PostPreferences

/** Правило скрытия из меню поста; список правил — пункт «Скрытые посты» в «Содержимое ›». */
internal sealed interface HiddenPostRule {
    val title: String

    data class Post(
        val id: Long,
    ) : HiddenPostRule {
        override val title: String get() = "Пост #$id"
    }

    data class Replies(
        val id: Long,
    ) : HiddenPostRule {
        override val title: String get() = "Пост #$id и ответы на него"
    }

    data class Name(
        val name: String,
    ) : HiddenPostRule {
        override val title: String get() = "Имя: $name"
    }

    data class Similar(
        val text: String,
    ) : HiddenPostRule {
        override val title: String get() = "Похожие на «${text.lineSequence().first().take(SIMILAR_PREVIEW)}»"
    }
}

internal fun PostPreferences.hiddenRules(): List<HiddenPostRule> =
    hidden.sorted().map(HiddenPostRule::Post) +
        replyTrees.sorted().map(HiddenPostRule::Replies) +
        names.sorted().map(HiddenPostRule::Name) +
        similar.map(HiddenPostRule::Similar)

internal fun PostPreferences.without(rule: HiddenPostRule): PostPreferences =
    when (rule) {
        is HiddenPostRule.Post -> copy(hidden = hidden - rule.id)
        is HiddenPostRule.Replies -> copy(replyTrees = replyTrees - rule.id)
        is HiddenPostRule.Name -> copy(names = names - rule.name)
        is HiddenPostRule.Similar -> copy(similar = similar - rule.text)
    }

private const val SIMILAR_PREVIEW = 40
