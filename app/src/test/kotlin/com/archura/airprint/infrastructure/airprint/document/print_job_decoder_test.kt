package com.archura.airprint.infrastructure.airprint.document

import com.archura.airprint.domain.model.DocumentFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrintJobDecoderTest {
    private val decoder = PrintJobDecoder()

    @Test
    fun testDetectJpegFormat() {
        val document = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02)
        val payload = "ipp-prefix".toByteArray() + document

        val decoded = decoder.decode(payload)

        assertEquals(DocumentFormat.JPEG, decoded?.format)
        assertArrayEquals(document, decoded?.bytes)
    }

    @Test
    fun testDetectPdfFormat() {
        val document = "%PDF-1.7".toByteArray()
        val payload = byteArrayOf(0x01, 0x02, 0x03) + document

        val decoded = decoder.decode(payload)

        assertEquals(DocumentFormat.PDF, decoded?.format)
    }

    @Test
    fun testRejectUnknownFormat() {
        val decoded = decoder.decode("not-a-document".toByteArray())

        assertNull(decoded)
    }
}
