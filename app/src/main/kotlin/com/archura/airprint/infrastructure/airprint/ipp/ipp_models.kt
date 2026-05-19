package com.archura.airprint.infrastructure.airprint.ipp

import com.archura.airprint.domain.model.IppOperation

data class IppRequest(
    val versionMajor: Int,
    val versionMinor: Int,
    val operation: IppOperation,
    val requestId: Int,
    val payload: ByteArray,
)

class IppParseException(message: String) : IllegalArgumentException(message)
