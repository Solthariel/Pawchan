package com.solthariel.pawchan.data.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardDirectoryPayloadDecoderTest {
    private val decoder =
        BoardDirectoryPayloadDecoder(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        )

    @Test
    fun decodesBoardsInServerCategoryOrder() {
        val boards = requireNotNull(decoder.decode(fixture("boards-success.json")))

        assertEquals(listOf("b", "d", "vn", "test"), boards.map { it.address.board.value })
        assertEquals(
            listOf("Разное", "Разное", "Японская культура", "Пользовательские"),
            boards.map { it.category },
        )
        assertEquals("Бред", boards.first().title)
        assertEquals(false, boards.first().requiresPostingAccess)
        assertEquals(false, boards[1].requiresPostingAccess)
        assertEquals(listOf(136, 0, 2, 4), boards.map { it.postsPerHour })
        assertEquals(false, boards[1].postingEnabled)
        assertEquals(true, boards[2].requiresPostingAccess)
    }

    @Test
    fun ignoresShieldFlagAndUsesConfirmedPostingGatePolicy() {
        val payload =
            """
            {"boards":[
              {"id":"2d","name":"Аниме/Беседка","enable_shield":true},
              {"id":"gb","name":"Азартные игры","enable_shield":true},
              {"id":"hc","name":"Hardcore","enable_shield":true},
              {"id":"fg","name":"Трапы","enable_shield":true},
              {"id":"vn","name":"Визуальные новеллы","enable_shield":false}
            ]}
            """.trimIndent()

        val access =
            requireNotNull(decoder.decode(payload))
                .associate { board -> board.address.board.value to board.requiresPostingAccess }

        assertEquals(false, access["2d"])
        assertEquals(false, access["gb"])
        assertEquals(false, access["hc"])
        assertEquals(true, access["fg"])
        assertEquals(true, access["vn"])
    }

    @Test
    fun skipsMalformedBoardRows() {
        val payload =
            """
            {"boards":[
              {"id":"b","name":"Бред","category":"Разное"},
              {"id":"bad code","name":"Не откроется","category":"Разное"},
              {"id":"x","name":"   ","category":"Разное"}
            ]}
            """.trimIndent()

        assertEquals(
            listOf("b"),
            requireNotNull(decoder.decode(payload)).map { it.address.board.value },
        )
        assertNull(decoder.decode("""{"boards":[{"id":"bad code","name":"Bad"}]}"""))
    }

    @Test
    fun requestUsesSingleBoardsEndpoint() {
        val request = BoardDirectoryRequestFactory.create()

        assertEquals("https://2ch.su/index.json", request.url.toString())
        assertEquals("GET", request.method)
    }

    private fun fixture(name: String): String = requireNotNull(javaClass.getResource("/fixtures/$name")).readText()
}
