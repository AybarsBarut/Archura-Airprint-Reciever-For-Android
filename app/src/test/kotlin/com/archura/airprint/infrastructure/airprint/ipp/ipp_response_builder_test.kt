package com.archura.airprint.infrastructure.airprint.ipp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IppResponseBuilderTest {
    private val builder = IppResponseBuilder()

    @Test
    fun testBuildOkResponse() {
        val response = builder.buildOkResponse(requestId = 42)

        assertArrayEquals(
            byteArrayOf(
                0x01,
                0x01,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x2A,
                0x03,
            ),
            response,
        )
    }

    @Test
    fun testBuildPrinterAttributesResponse() {
        val response = builder.buildPrinterAttributesResponse(
            requestId = 42,
            printerName = "Archura AirPrint Receiver",
            printerUri = "ipp://192.168.1.14:9100/ipp/print",
        )

        val responseText = response.toString(Charsets.ISO_8859_1)
        assertTrue(responseText.contains("printer-state"))
        assertTrue(responseText.contains("printer-is-accepting-jobs"))
        assertTrue(responseText.contains("document-format-supported"))
        assertTrue(responseText.contains("urf-supported"))
    }
}
