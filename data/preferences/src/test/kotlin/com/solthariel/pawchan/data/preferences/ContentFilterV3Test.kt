package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.isBuilderMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64

class ContentFilterV3Test {
    @Test fun oldRecordsAreDiscardedRatherThanMigrated() {
        for (version in listOf("1", "2", "3", "4")) {
            val fields = listOf(version, "old", "dvach", "b", "true", "true", "true", "EITHER", "REGEX", "кот")
            val record = fields.joinToString("\t") { Base64.getEncoder().encodeToString(it.toByteArray()) }
            assertEquals(emptyList<ContentFilterRule>(), decodeContentFilters(record))
        }
    }

    @Test fun oldModesCannotBeWrittenBackUnderANewVersion() {
        for (mode in ContentFilterMode.entries.filterNot { it.isBuilderMode }) {
            val rule = ContentFilterRule("old", "dvach", condition = ContentFilterConditionSpec(mode, listOf("кот")))
            assertEquals("", encodeContentFilters(listOf(rule)))
        }
    }
}
