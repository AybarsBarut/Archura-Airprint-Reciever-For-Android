package com.archura.airprint.domain.model

data class ReceivedImage(
    val id: String,
    val path: String,
    val fileName: String,
    val timestampMillis: Long,
    val format: DocumentFormat,
    val senderAddress: String?,
    val sizeBytes: Long,
)
