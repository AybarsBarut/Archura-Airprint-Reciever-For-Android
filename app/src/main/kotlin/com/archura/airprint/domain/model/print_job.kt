package com.archura.airprint.domain.model

data class PrintJob(
    val id: String,
    val operation: IppOperation,
    val senderAddress: String?,
    val documentFormat: DocumentFormat,
    val receivedAtMillis: Long,
    val sizeBytes: Long,
)
