package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentFilterRecipesTest {
    @Test fun explicitAlternativesHaveWholePostException() {
        val rule =
            builderRule(
                ContentFilterMode.ANY_TERM,
                listOf("беларус, беларуса, беларусы"),
                exceptions = listOf("известный, известного"),
            )
        val engine = ContentFilterEngine(listOf(rule))
        listOf("Почему нет ни одного беларуса?", "БЕЛАРУСЫ", "Неизвестный беларус", "Беларус\nи культура").forEach {
            assertNotNull(it, engine.comment(builderBoard, it))
        }
        listOf("Известный беларус", "беларус, известного всем", "Известного\nбеларуса", "небеларус").forEach {
            assertNull(it, engine.comment(builderBoard, it))
        }
    }

    @Test fun allWordsWithExceptionCoversOrderAndNewlines() {
        val engine =
            ContentFilterEngine(
                listOf(
                    builderRule(
                        ContentFilterMode.ALL_TERMS,
                        listOf("кот", "собака"),
                        exceptions = listOf("приют"),
                    ),
                ),
            )
        listOf("КОТ ищет дом.\nРядом собака.", "собака и кот").forEach {
            assertNotNull(engine.comment(builderBoard, it))
        }
        listOf("Приют кот собака", "кот\nсобака ПРИЮТ", "котик и собака", "только кот").forEach {
            assertNull(engine.comment(builderBoard, it))
        }
    }
}
