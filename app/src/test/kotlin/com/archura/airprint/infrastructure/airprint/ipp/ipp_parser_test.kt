package com.archura.airprint.infrastructure.airprint.ipp

import com.archura.airprint.domain.model.IppOperation
import org.junit.Assert.assertEquals
import org.junit.Test

class IppParserTest {
    private val parser = IppParser()

    @Test
    fun testParseValidPrintJobRequest() {
        val requestBytes = byteArrayOf(
            0x01,
            0x01,
            0x00,
            0x02,
            0x00,
            0x00,
            0x00,
            0x2A,
            0x03,
        )

        val request = parser.parse(requestBytes)

        assertEquals(1, request.versionMajor)
        assertEquals(1, request.versionMinor)
        assertEquals(IppOperation.PRINT_JOB, request.operation)
        assertEquals(42, request.requestId)
    }

    @Test(expected = IppParseException::class)
    fun testParseInvalidMagicNumber() {
        parser.parse(byteArrayOf(0x01))
    }
}
