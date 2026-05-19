package com.archura.airprint.domain.model

enum class DocumentFormat(
    val displayName: String,
    val mimeType: String,
    val extension: String,
) {
    JPEG("JPEG", "image/jpeg", "jpg"),
    PNG("PNG", "image/png", "png"),
    TXT("Text", "text/plain", "txt"),
    PDF("PDF", "application/pdf", "pdf"),
    URF("URF", "image/urf", "urf"),
    PWG_RASTER("PWG Raster", "image/pwg-raster", "ras"),
    UNKNOWN("Unknown", "application/octet-stream", "bin"),
}
