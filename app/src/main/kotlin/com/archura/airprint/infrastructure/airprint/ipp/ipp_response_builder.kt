package com.archura.airprint.infrastructure.airprint.ipp

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

class IppResponseBuilder @Inject constructor() {
    fun buildOkResponse(requestId: Int): ByteArray {
        return buildResponse(
            statusCode = IppConstants.STATUS_OK,
            requestId = requestId,
        )
    }

    fun buildOperationOkResponse(requestId: Int): ByteArray {
        return buildResponse(
            statusCode = IppConstants.STATUS_OK,
            requestId = requestId,
        ) {
            group(IppConstants.ATTRIBUTE_TAG_OPERATION) {
                charset("attributes-charset", "utf-8")
                naturalLanguage("attributes-natural-language", "en")
            }
        }
    }

    fun buildPrinterAttributesResponse(
        requestId: Int,
        printerName: String,
        printerUri: String,
    ): ByteArray {
        return buildResponse(
            statusCode = IppConstants.STATUS_OK,
            requestId = requestId,
        ) {
            group(IppConstants.ATTRIBUTE_TAG_OPERATION) {
                charset("attributes-charset", "utf-8")
                naturalLanguage("attributes-natural-language", "en")
            }
            group(IppConstants.ATTRIBUTE_TAG_PRINTER) {
                uri("printer-uri-supported", printerUri)
                keyword("uri-authentication-supported", "none")
                keyword("uri-security-supported", "none")
                name("printer-name", printerName)
                text("printer-info", "Android AirPrint receiver")
                text("printer-make-and-model", "Archura AirPrint Receiver")
                enum("printer-state", PRINTER_STATE_IDLE)
                keyword("printer-state-reasons", "none")
                boolean("printer-is-accepting-jobs", true)
                integer("queued-job-count", 0)
                keyword("ipp-versions-supported", listOf("1.1", "2.0"))
                enum(
                    "operations-supported",
                    listOf(
                        IppConstants.OPERATION_PRINT_JOB,
                        IppConstants.OPERATION_GET_PRINTER_ATTRIBUTES,
                        OPERATION_VALIDATE_JOB,
                        OPERATION_CREATE_JOB,
                        OPERATION_SEND_DOCUMENT,
                        OPERATION_GET_JOBS,
                        OPERATION_GET_JOB_ATTRIBUTES,
                        OPERATION_CANCEL_JOB,
                    ),
                )
                boolean("multiple-document-jobs-supported", false)
                charset("charset-configured", "utf-8")
                charset("charset-supported", "utf-8")
                naturalLanguage("natural-language-configured", "en")
                naturalLanguage("generated-natural-language-supported", "en")
                mimeMediaType("document-format-default", "application/pdf")
                mimeMediaType(
                    "document-format-supported",
                    listOf(
                        "application/pdf",
                        "image/jpeg",
                        "image/urf",
                        "image/pwg-raster",
                    ),
                )
                keyword("compression-supported", "none")
                keyword("pdl-override-supported", "not-attempted")
                boolean("color-supported", true)
                keyword("print-color-mode-default", "color")
                keyword("print-color-mode-supported", listOf("color", "monochrome"))
                keyword("sides-default", "one-sided")
                keyword("sides-supported", "one-sided")
                rangeOfInteger("copies-supported", 1, 1)
                keyword("media-default", "iso_a4_210x297mm")
                keyword("media-supported", listOf("iso_a4_210x297mm", "na_letter_8.5x11in"))
                enum("print-quality-default", PRINT_QUALITY_NORMAL)
                enum(
                    "print-quality-supported",
                    listOf(
                        PRINT_QUALITY_DRAFT,
                        PRINT_QUALITY_NORMAL,
                        PRINT_QUALITY_HIGH,
                    ),
                )
                resolution("printer-resolution-default", 300, 300, RESOLUTION_UNIT_DPI)
                resolution("printer-resolution-supported", 300, 300, RESOLUTION_UNIT_DPI)
                keyword(
                    "urf-supported",
                    listOf("CP1", "IS1-4-5", "MT1-2-3-4-5-6", "RS600", "V1.4", "W8", "SRGB24"),
                )
            }
        }
    }

    fun buildPrintJobAcceptedResponse(
        requestId: Int,
        jobId: Int,
        printerUri: String,
    ): ByteArray {
        return buildResponse(
            statusCode = IppConstants.STATUS_OK,
            requestId = requestId,
        ) {
            group(IppConstants.ATTRIBUTE_TAG_OPERATION) {
                charset("attributes-charset", "utf-8")
                naturalLanguage("attributes-natural-language", "en")
            }
            group(IppConstants.ATTRIBUTE_TAG_JOB) {
                uri("job-uri", "$printerUri/jobs/$jobId")
                integer("job-id", jobId)
                enum("job-state", JOB_STATE_COMPLETED)
                keyword("job-state-reasons", "none")
            }
        }
    }

    fun buildBadRequestResponse(requestId: Int): ByteArray {
        return buildResponse(
            statusCode = IppConstants.STATUS_CLIENT_ERROR_BAD_REQUEST,
            requestId = requestId,
        )
    }

    private fun buildResponse(
        statusCode: Int,
        requestId: Int,
        attributes: IppAttributesWriter.() -> Unit = {},
    ): ByteArray {
        val attributeBytes = IppAttributesWriter().apply(attributes).toByteArray()

        return ByteArrayOutputStream().apply {
            write(IppConstants.VERSION_MAJOR)
            write(IppConstants.VERSION_MINOR)
            writeShort(statusCode)
            writeInt(requestId)
            write(attributeBytes)
            write(IppConstants.END_OF_ATTRIBUTES_TAG)
        }.toByteArray()
    }

    @Suppress("TooManyFunctions")
    private class IppAttributesWriter {
        private val output = ByteArrayOutputStream()

        fun group(groupTag: Int, block: IppAttributesWriter.() -> Unit) {
            output.write(groupTag)
            block()
        }

        fun boolean(name: String, value: Boolean) {
            writeAttribute(VALUE_TAG_BOOLEAN, name, byteArrayOf(if (value) 1 else 0))
        }

        fun charset(name: String, value: String) {
            writeStringAttribute(VALUE_TAG_CHARSET, name, listOf(value))
        }

        fun enum(name: String, value: Int) {
            enum(name, listOf(value))
        }

        fun enum(name: String, values: List<Int>) {
            values.forEachIndexed { index, value ->
                writeAttribute(
                    valueTag = VALUE_TAG_ENUM,
                    name = name.takeIf { index == 0 }.orEmpty(),
                    value = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).array(),
                )
            }
        }

        fun integer(name: String, value: Int) {
            writeAttribute(
                valueTag = VALUE_TAG_INTEGER,
                name = name,
                value = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).array(),
            )
        }

        fun keyword(name: String, value: String) {
            keyword(name, listOf(value))
        }

        fun keyword(name: String, values: List<String>) {
            writeStringAttribute(VALUE_TAG_KEYWORD, name, values)
        }

        fun mimeMediaType(name: String, value: String) {
            mimeMediaType(name, listOf(value))
        }

        fun mimeMediaType(name: String, values: List<String>) {
            writeStringAttribute(VALUE_TAG_MIME_MEDIA_TYPE, name, values)
        }

        fun name(name: String, value: String) {
            writeStringAttribute(VALUE_TAG_NAME_WITHOUT_LANGUAGE, name, listOf(value))
        }

        fun naturalLanguage(name: String, value: String) {
            writeStringAttribute(VALUE_TAG_NATURAL_LANGUAGE, name, listOf(value))
        }

        fun rangeOfInteger(
            name: String,
            lower: Int,
            upper: Int,
        ) {
            writeAttribute(
                valueTag = VALUE_TAG_RANGE_OF_INTEGER,
                name = name,
                value = ByteBuffer.allocate(Int.SIZE_BYTES * 2)
                    .putInt(lower)
                    .putInt(upper)
                    .array(),
            )
        }

        fun resolution(
            name: String,
            crossFeedResolution: Int,
            feedResolution: Int,
            unit: Int,
        ) {
            writeAttribute(
                valueTag = VALUE_TAG_RESOLUTION,
                name = name,
                value = ByteBuffer.allocate((Int.SIZE_BYTES * 2) + 1)
                    .putInt(crossFeedResolution)
                    .putInt(feedResolution)
                    .put(unit.toByte())
                    .array(),
            )
        }

        fun text(name: String, value: String) {
            writeStringAttribute(VALUE_TAG_TEXT_WITHOUT_LANGUAGE, name, listOf(value))
        }

        fun uri(name: String, value: String) {
            writeStringAttribute(VALUE_TAG_URI, name, listOf(value))
        }

        fun toByteArray(): ByteArray {
            return output.toByteArray()
        }

        private fun writeStringAttribute(
            valueTag: Int,
            name: String,
            values: List<String>,
        ) {
            values.forEachIndexed { index, value ->
                writeAttribute(
                    valueTag = valueTag,
                    name = name.takeIf { index == 0 }.orEmpty(),
                    value = value.toByteArray(Charsets.UTF_8),
                )
            }
        }

        private fun writeAttribute(
            valueTag: Int,
            name: String,
            value: ByteArray,
        ) {
            output.write(valueTag)
            output.writeShort(name.toByteArray(Charsets.UTF_8).size)
            output.write(name.toByteArray(Charsets.UTF_8))
            output.writeShort(value.size)
            output.write(value)
        }
    }

    private companion object {
        const val JOB_STATE_COMPLETED = 9
        const val OPERATION_CANCEL_JOB = 0x0008
        const val OPERATION_CREATE_JOB = 0x0005
        const val OPERATION_GET_JOB_ATTRIBUTES = 0x0009
        const val OPERATION_GET_JOBS = 0x000A
        const val OPERATION_SEND_DOCUMENT = 0x0006
        const val OPERATION_VALIDATE_JOB = 0x0004
        const val PRINT_QUALITY_DRAFT = 3
        const val PRINT_QUALITY_HIGH = 5
        const val PRINT_QUALITY_NORMAL = 4
        const val PRINTER_STATE_IDLE = 3
        const val RESOLUTION_UNIT_DPI = 3
        const val VALUE_TAG_BOOLEAN = 0x22
        const val VALUE_TAG_CHARSET = 0x47
        const val VALUE_TAG_ENUM = 0x23
        const val VALUE_TAG_INTEGER = 0x21
        const val VALUE_TAG_KEYWORD = 0x44
        const val VALUE_TAG_MIME_MEDIA_TYPE = 0x49
        const val VALUE_TAG_NAME_WITHOUT_LANGUAGE = 0x42
        const val VALUE_TAG_NATURAL_LANGUAGE = 0x48
        const val VALUE_TAG_RANGE_OF_INTEGER = 0x33
        const val VALUE_TAG_RESOLUTION = 0x32
        const val VALUE_TAG_TEXT_WITHOUT_LANGUAGE = 0x41
        const val VALUE_TAG_URI = 0x45
    }
}

private fun ByteArrayOutputStream.writeInt(value: Int) {
    write(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).array())
}

private fun ByteArrayOutputStream.writeShort(value: Int) {
    write(ByteBuffer.allocate(Short.SIZE_BYTES).putShort(value.toShort()).array())
}
