package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterFields
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.domain.filter.contentFilterRecordValidationError
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import java.util.Properties

internal fun writeBackupRule(
    data: Properties,
    prefix: String,
    rule: ContentFilterRule,
) {
    val fields = BackupFields(data, prefix)
    fields.put("id", rule.id)
    fields.put("site", rule.site)
    fields.putList("boards", rule.boards.toList())
    fields.put("enabled", rule.enabled)
    fields.put("op", rule.threads)
    fields.put("replies", rule.comments)
    fields.put("fields", rule.fields.name)
    fields.put("catalog", rule.targets.catalog)
    fields.put("thread", rule.targets.thread)
    fields.putList("threadIds", rule.targets.threadIds.map(Long::toString))
    fields.put("mode", rule.mode.name)
    fields.putList("values", rule.values)
    rule.options.customLabel?.let { fields.put("label", it) }
    fields.put("wholeWords", rule.options.wholeWords)
    rule.options.maxGap?.let { fields.put("gap", it) }
    fields.putList("exceptions", rule.options.exceptions)
    rule.options.expiresAtMillis?.let { fields.put("expires", it) }
}

internal fun readBackupRule(fields: BackupFields): ContentFilterRule {
    val rule =
        ContentFilterRule(
            id = fields.text("id"),
            site = fields.text("site"),
            boards = fields.list("boards").toSet(),
            enabled = fields.boolean("enabled"),
            targets = readBackupTargets(fields),
            condition =
                ContentFilterConditionSpec(ContentFilterMode.valueOf(fields.text("mode")), fields.list("values")),
            options =
                ContentFilterOptions(
                    fields.optional("label"),
                    fields.boolean("wholeWords"),
                    fields.optional("gap")?.toInt(),
                    fields.list("exceptions"),
                    fields.optional("expires")?.toLong(),
                ),
        )
    require(rule.id.isNotBlank() && SiteId.parse(rule.site)?.value == rule.site) { "Некорректный адрес правила" }
    require(rule.boards.all { BoardId.parse(it)?.value == it }) { "Некорректная доска" }
    require(rule.targets.threadIds.all { it > 0L }) { "Некорректный номер треда" }
    require(contentFilterRecordValidationError(rule) == null) { "Некорректное условие правила" }
    return rule
}

private fun readBackupTargets(fields: BackupFields): ContentFilterTargets =
    ContentFilterTargets(
        fields.boolean("op"),
        fields.boolean("replies"),
        ContentFilterFields.valueOf(fields.text("fields")),
        fields.boolean("catalog"),
        fields.boolean("thread"),
        fields.list("threadIds").map(String::toLong).toSet(),
    )

internal class BackupFields(
    private val properties: Properties,
    private val prefix: String,
) {
    fun text(name: String): String = requireNotNull(optional(name)) { "Отсутствует поле $prefix$name" }

    fun optional(name: String): String? = properties.getProperty(prefix + name)

    fun boolean(name: String): Boolean = text(name).toBooleanStrict()

    fun list(name: String): List<String> {
        val count = text("$name.count").toInt()
        require(count in 0..FILTER_BACKUP_MAX_ITEMS) { "Некорректная длина списка" }
        return List(count) { text("$name.$it") }
    }

    fun put(
        name: String,
        value: Any,
    ) {
        properties.setProperty(prefix + name, value.toString())
    }

    fun putList(
        name: String,
        values: List<String>,
    ) {
        require(values.size <= FILTER_BACKUP_MAX_ITEMS) { "Слишком длинный список в правиле" }
        put("$name.count", values.size)
        values.forEachIndexed { index, value -> put("$name.$index", value) }
    }
}
