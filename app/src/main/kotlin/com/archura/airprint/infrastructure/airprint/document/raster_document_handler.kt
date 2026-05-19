package com.archura.airprint.infrastructure.airprint.document

import com.archura.airprint.domain.model.DocumentFormat

class RasterDocumentHandler {
    fun canHandle(decodedDocument: DecodedDocument): Boolean {
        return decodedDocument.format == DocumentFormat.URF ||
            decodedDocument.format == DocumentFormat.PWG_RASTER
    }
}
