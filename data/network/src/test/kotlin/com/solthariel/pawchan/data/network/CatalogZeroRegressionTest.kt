package com.solthariel.pawchan.data.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogZeroRegressionTest {
    @Test fun explicitServerZeroMustNotBeOverwrittenByAnotherSpeedField() {
        val result =
            requireNotNull(
                CatalogPayloadDecoder(Json { ignoreUnknownKeys = true }).decode(
                    """{"board_speed":0,"speed":999,"board":{"id":"b","name":"Бред"},"threads":[]}""",
                ),
            )
        assertEquals(0, result.boardSpeed)
    }
}
