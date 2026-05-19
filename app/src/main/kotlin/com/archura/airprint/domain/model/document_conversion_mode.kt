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
        description = "Render the first PDF page as a JPEG image for the gallery.",
    );

    companion object {
        fun fromStoredValue(value: String?): DocumentConversionMode {
            return entries.firstOrNull { mode -> mode.name == value } ?: KEEP_ORIGINAL
        }
    }
}
