package com.archura.airprint.infrastructure.airprint.document

import com.archura.airprint.domain.model.DocumentFormat

class DocumentFormatDetector {
    fun detect(documentBytes: ByteArray): DocumentFormat {
        return when {
            documentBytes.startsWith(JPEG_MAGIC) -> DocumentFormat.JPEG
            documentBytes.startsWith(PDF_MAGIC) -> DocumentFormat.PDF
            documentBytes.startsWith(URF_MAGIC) -> DocumentFormat.URF
            documentBytes.startsWith(PWG_RASTER_MAGIC) -> DocumentFormat.PWG_RASTER
            else -> DocumentFormat.UNKNOWN
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        return size >= prefix.size && prefix.indices.all { index -> this[index] == prefix[index] }
    }

    private companion object {
        val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        val PDF_MAGIC = "%PDF".toByteArray()
        val PWG_RASTER_MAGIC = "RaS2".toByteArray()
        val URF_MAGIC = "UNIRAST".toByteArray()
    }
}
