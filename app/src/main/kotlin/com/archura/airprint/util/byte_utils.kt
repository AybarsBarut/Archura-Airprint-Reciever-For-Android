package com.archura.airprint.util

fun ByteArray.toHexPreview(maxBytes: Int = DEFAULT_PREVIEW_BYTES): String {
    return take(maxBytes).joinToString(separator = " ") { byte ->
        "%02X".format(byte)
    }
}

private const val DEFAULT_PREVIEW_BYTES = 32
