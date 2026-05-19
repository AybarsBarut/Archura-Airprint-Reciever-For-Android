package com.archura.airprint.domain.model

enum class DocumentConversionMode(
    val displayName: String,
    val description: String,
) {
    KEEP_ORIGINAL(
        displayName = "Keep original",
        description = "Save AirPrint files in the format received from iOS or macOS.",
    ),
    PDF_FIRST_PAGE_TO_JPEG(
        displayName = "PDF to JPEG",
        description = "Render all PDF pages as separate JPEG images for the gallery.",
    ),
    PDF_TO_PNG(
        displayName = "PDF to PNG",
        description = "Render all PDF pages as separate PNG images for the gallery.",
    );

    companion object {
        fun fromStoredValue(value: String?): DocumentConversionMode {
            return entries.firstOrNull { mode -> mode.name == value } ?: KEEP_ORIGINAL
        }
    }
}
