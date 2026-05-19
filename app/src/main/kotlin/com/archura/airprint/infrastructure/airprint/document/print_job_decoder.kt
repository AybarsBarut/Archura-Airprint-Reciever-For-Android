package com.archura.airprint.infrastructure.airprint.document

import com.archura.airprint.domain.model.DocumentFormat
import javax.inject.Inject

class PrintJobDecoder @Inject constructor() {
    fun decode(ippPayload: ByteArray): DecodedDocument? {
        val documentStart = listOfNotNull(
            ippPayload.indexOfSequence(JPEG_MAGIC).takeIf { index -> index >= 0 },
            ippPayload.indexOfSequence(PDF_MAGIC).takeIf { index -> index >= 0 },
            ippPayload.indexOfSequence(URF_MAGIC).takeIf { index -> index >= 0 },
            ippPayload.indexOfSequence(PWG_RASTER_MAGIC).takeIf { index -> index >= 0 },
        ).minOrNull() ?: return null

        val documentBytes = ippPayload.copyOfRange(documentStart, ippPayload.size)
        val format = DocumentFormatDetector().detect(documentBytes)
        if (format == DocumentFormat.UNKNOWN) {
            return null
        }

        return DecodedDocument(
            format = format,
            bytes = documentBytes,
        )
    }

    private fun ByteArray.indexOfSequence(pattern: ByteArray): Int {
        if (pattern.isEmpty() || size < pattern.size) {
            return -1
        }

        for (startIndex in 0..(size - pattern.size)) {
            val matches = pattern.indices.all { patternIndex ->
                this[startIndex + patternIndex] == pattern[patternIndex]
            }
            if (matches) {
                return startIndex
            }
        }

        return -1
    }

    private companion object {
        val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        val PDF_MAGIC = "%PDF".toByteArray()
        val PWG_RASTER_MAGIC = "RaS2".toByteArray()
        val URF_MAGIC = "UNIRAST".toByteArray()
    }
}

data class DecodedDocument(
    val format: DocumentFormat,
    val bytes: ByteArray,
)
