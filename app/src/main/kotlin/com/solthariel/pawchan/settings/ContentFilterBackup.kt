package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterRule
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import java.util.UUID

internal const val FILTER_BACKUP_MAX_BYTES = 1024 * 1024
internal const val FILTER_BACKUP_MAX_ITEMS = 2000

internal fun encodeContentFilterBackup(rules: List<ContentFilterRule>): String {
    require(rules.size <= FILTER_BACKUP_MAX_ITEMS) { "Слишком много правил" }
    val data = Properties()
    data.setProperty("format", "pawchan-autohide")
    data.setProperty("version", "2")
    data.setProperty("rules", rules.size.toString())
    rules.forEachIndexed { index, rule -> writeBackupRule(data, "rule.$index.", rule) }
    val text = StringWriter().also { data.store(it, "Pawchan autohide - UTF-8") }.toString()
    require(text.toByteArray(Charsets.UTF_8).size <= FILTER_BACKUP_MAX_BYTES) { "Файл больше 1 МБ" }
    return text
}

internal fun decodeContentFilterBackup(source: String): List<ContentFilterRule> {
    require(source.length <= FILTER_BACKUP_MAX_BYTES) { "Файл больше 1 МБ" }
    val data = UniqueBackupProperties()
    data.load(StringReader(source.removePrefix("\uFEFF")))
    require(data.getProperty("format") == "pawchan-autohide") { "Это не резервная копия правил Pawchan" }
    require(data.getProperty("version") == "2") { "Неподдерживаемая версия резервной копии" }
    val count = requireNotNull(data.getProperty("rules")).toInt()
    require(count in 0..FILTER_BACKUP_MAX_ITEMS) { "Некорректное число правил" }
    return List(count) { index -> readBackupRule(BackupFields(data, "rule.$index.")) }
}

internal fun mergeContentFilterBackup(
    current: List<ContentFilterRule>,
    incoming: List<ContentFilterRule>,
): List<ContentFilterRule> {
    val result = current.toMutableList()
    val ids = current.map { it.id }.toMutableSet()
    val signatures = current.map { it.copy(id = "") }.toMutableSet()
    incoming.forEach { rule ->
        if (signatures.add(rule.copy(id = ""))) {
            val id = if (rule.id in ids) freshBackupRuleId(ids) else rule.id
            ids.add(id)
            result.add(rule.copy(id = id))
        }
    }
    return result
}

private fun freshBackupRuleId(ids: Set<String>): String {
    var id = UUID.randomUUID().toString()
    while (id in ids) id = UUID.randomUUID().toString()
    return id
}

private class UniqueBackupProperties : Properties() {
    override fun put(
        key: Any,
        value: Any,
    ): Any? {
        require(!containsKey(key)) { "Повторяющееся поле: $key" }
        return super.put(key, value)
    }
}
