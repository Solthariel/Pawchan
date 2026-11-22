package io.github.yuroyami.libmpvkt

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NodeCodecTest {
    @Test
    fun nestedValuesRoundTrip() {
        val node =
            MpvNode.Dict(
                linkedMapOf(
                    "text" to MpvNode.Str("Привет"),
                    "values" to MpvNode.Arr(listOf(MpvNode.Int64(-123), MpvNode.Dbl(3.5), MpvNode.Flag(true))),
                    "bytes" to MpvNode.Bytes(byteArrayOf(0, -1, 1)),
                    "none" to MpvNode.None,
                ),
            )
        assertEquals(node, NodeCodec.decode(NodeCodec.encode(node)))
    }

    @Test
    fun int64UsesLittleEndianWireFormat() {
        val encoded = NodeCodec.encode(MpvNode.Int64(0x0102030405060708L))
        assertArrayEquals(byteArrayOf(4, 8, 7, 6, 5, 4, 3, 2, 1), encoded)
    }

    @Test
    fun rejectsTruncatedNode() {
        assertThrows(IllegalArgumentException::class.java) {
            NodeCodec.decode(byteArrayOf(4, 1, 2))
        }
    }

    @Test
    fun rejectsImpossibleMapLength() {
        assertThrows(IllegalArgumentException::class.java) {
            NodeCodec.decode(byteArrayOf(8, 1, 0, 0, 0))
        }
    }
}
