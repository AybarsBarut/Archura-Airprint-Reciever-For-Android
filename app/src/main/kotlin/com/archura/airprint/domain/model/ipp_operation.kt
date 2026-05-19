package com.archura.airprint.domain.model

enum class IppOperation(
    val id: Int,
) {
    GET_PRINTER_ATTRIBUTES(0x000B),
    PRINT_JOB(0x0002),
    VALIDATE_JOB(0x0004),
    CREATE_JOB(0x0005),
    SEND_DOCUMENT(0x0006),
    GET_JOBS(0x000A),
    GET_JOB_ATTRIBUTES(0x0009),
    CANCEL_JOB(0x0008),
    UNKNOWN(-1);

    companion object {
        fun fromId(id: Int): IppOperation {
            return entries.firstOrNull { operation -> operation.id == id } ?: UNKNOWN
        }
    }
}
