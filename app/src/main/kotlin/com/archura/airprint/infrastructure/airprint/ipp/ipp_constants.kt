package com.archura.airprint.infrastructure.airprint.ipp

object IppConstants {
    const val ATTRIBUTE_TAG_OPERATION = 0x01
    const val ATTRIBUTE_TAG_JOB = 0x02
    const val END_OF_ATTRIBUTES_TAG = 0x03
    const val ATTRIBUTE_TAG_PRINTER = 0x04
    const val OPERATION_GET_PRINTER_ATTRIBUTES = 0x000B
    const val OPERATION_PRINT_JOB = 0x0002
    const val STATUS_CLIENT_ERROR_BAD_REQUEST = 0x0400
    const val STATUS_OK = 0x0000
    const val VERSION_MAJOR = 0x01
    const val VERSION_MINOR = 0x01
}
