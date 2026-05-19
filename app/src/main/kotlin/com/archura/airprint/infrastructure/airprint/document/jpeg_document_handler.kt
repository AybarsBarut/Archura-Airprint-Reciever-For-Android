package com.archura.airprint.infrastructure.airprint.document

class JpegDocumentHandler {
    fun canHandle(decodedDocument: DecodedDocument): Boolean {
        return decodedDocument.format == com.archura.airprint.domain.model.DocumentFormat.JPEG
    }
}
