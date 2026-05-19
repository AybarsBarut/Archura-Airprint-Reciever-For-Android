package com.archura.airprint.infrastructure.airprint.ipp

import com.archura.airprint.domain.model.IppOperation
import javax.inject.Inject

class IppParser @Inject constructor() {
    fun parse(requestBytes: ByteArray): IppRequest {
        if (requestBytes.size < HEADER_LENGTH) {
            throw IppParseException("IPP request shorter than header")
        }

        val versionMajor = requestBytes[0].toUnsignedInt()
        val versionMinor = requestBytes[1].toUnsignedInt()
        val operationId = requestBytes.readUnsignedShort(offset = 2)
        val requestId = requestBytes.readInt(offset = 4)

        return IppRequest(
            versionMajor = versionMajor,
            versionMinor = versionMinor,
            operation = IppOperation.fromId(operationId),
            requestId = requestId,
            payload = requestBytes,
        )
    }

    private fun Byte.toUnsignedInt(): Int {
        return toInt() and BYTE_MASK
    }

    private fun ByteArray.readUnsignedShort(offset: Int): Int {
        return (this[offset].toUnsignedInt() shl BYTE_BITS) or this[offset + 1].toUnsignedInt()
    }

    private fun ByteArray.readInt(offset: Int): Int {
        return (this[offset].toUnsignedInt() shl 24) or
            (this[offset + 1].toUnsignedInt() shl 16) or
            (this[offset + 2].toUnsignedInt() shl BYTE_BITS) or
            this[offset + 3].toUnsignedInt()
    }

    private companion object {
        const val BYTE_BITS = 8
        const val BYTE_MASK = 0xFF
        const val HEADER_LENGTH = 8
    }
}
