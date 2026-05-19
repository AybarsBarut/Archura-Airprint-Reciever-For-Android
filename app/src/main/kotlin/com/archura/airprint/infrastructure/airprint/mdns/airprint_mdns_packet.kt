package com.archura.airprint.infrastructure.airprint.mdns

import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.nio.ByteBuffer
import java.util.Locale

class AirPrintMdnsPacket(
    private val serviceName: String,
    private val hostName: String,
    private val port: Int,
    private val localAddress: Inet4Address,
    private val ippTxtRecords: Map<String, String>,
    private val uscanTxtRecords: Map<String, String>,
) {
    private val ippServiceInstance = "${serviceName.sanitizeDnsLabel()}.$IPP_SERVICE_TYPE"
    private val uscanServiceInstance = "${(serviceName + " Scanner").sanitizeDnsLabel()}.$USCAN_SERVICE_TYPE"
    private val uscansServiceInstance = "${(serviceName + " Scanner").sanitizeDnsLabel()}.$USCANS_SERVICE_TYPE"
    private val host = "${hostName.sanitizeDnsLabel()}.$LOCAL_DOMAIN"

    fun buildAnnouncement(): ByteArray {
        return buildResponse()
    }

    fun buildResponseIfRelevant(queryBytes: ByteArray): ByteArray? {
        val query = MdnsQuery.parse(queryBytes) ?: return null
        query.questions.forEach { question ->
            android.util.Log.d("AirPrintMdnsPacket", "Received mDNS query for: ${question.name}")
        }
        if (!query.questions.any { question -> question.isAirPrintQuestion() }) {
            return null
        }

        return buildResponse()
    }

    private fun buildResponse(): ByteArray {
        val records = listOf(
            // IPP Records
            ResourceRecord.ptr(IPP_SERVICE_TYPE, ippServiceInstance),
            ResourceRecord.ptr(UNIVERSAL_SUBTYPE, ippServiceInstance),
            ResourceRecord.srv(ippServiceInstance, host, port),
            ResourceRecord.txt(ippServiceInstance, ippTxtRecords),

            // UScan (Non-secure Scanner) Records
            ResourceRecord.ptr(USCAN_SERVICE_TYPE, uscanServiceInstance),
            ResourceRecord.srv(uscanServiceInstance, host, port),
            ResourceRecord.txt(uscanServiceInstance, uscanTxtRecords),

            // UScans (Secure Scanner) Records
            ResourceRecord.ptr(USCANS_SERVICE_TYPE, uscansServiceInstance),
            ResourceRecord.srv(uscansServiceInstance, host, port),
            ResourceRecord.txt(uscansServiceInstance, uscanTxtRecords),

            // Shared Host A Record
            ResourceRecord.a(host, localAddress),
        )

        return ByteArrayOutputStream().apply {
            writeShort(0)
            writeShort(RESPONSE_FLAGS)
            writeShort(0)
            writeShort(records.size)
            writeShort(0)
            writeShort(0)
            records.forEach { record -> write(record.toBytes()) }
        }.toByteArray()
    }

    private fun MdnsQuestion.isAirPrintQuestion(): Boolean {
        val normalized = name.normalizeDnsName()
        
        val ippSpace = "${serviceName}.$IPP_SERVICE_TYPE".normalizeDnsName()
        val ippHyphen = ippServiceInstance.normalizeDnsName()
        
        val uscanSpace = "${serviceName} Scanner.$USCAN_SERVICE_TYPE".normalizeDnsName()
        val uscanHyphen = uscanServiceInstance.normalizeDnsName()
        
        val uscansSpace = "${serviceName} Scanner.$USCANS_SERVICE_TYPE".normalizeDnsName()
        val uscansHyphen = uscansServiceInstance.normalizeDnsName()
        
        return normalized == IPP_SERVICE_TYPE.normalizeDnsName() ||
            normalized == UNIVERSAL_SUBTYPE.normalizeDnsName() ||
            normalized == USCAN_SERVICE_TYPE.normalizeDnsName() ||
            normalized == USCANS_SERVICE_TYPE.normalizeDnsName() ||
            normalized == ippSpace ||
            normalized == ippHyphen ||
            normalized == uscanSpace ||
            normalized == uscanHyphen ||
            normalized == uscansSpace ||
            normalized == uscansHyphen ||
            normalized == host.normalizeDnsName()
    }

    private data class ResourceRecord(
        val name: String,
        val type: Int,
        val dnsClass: Int,
        val ttlSeconds: Int,
        val data: ByteArray,
    ) {
        fun toBytes(): ByteArray {
            return ByteArrayOutputStream().apply {
                writeDnsName(name)
                writeShort(type)
                writeShort(dnsClass)
                writeInt(ttlSeconds)
                writeShort(data.size)
                write(data)
            }.toByteArray()
        }

        companion object {
            fun ptr(name: String, target: String): ResourceRecord {
                return ResourceRecord(
                    name = name,
                    type = TYPE_PTR,
                    dnsClass = CLASS_IN,
                    ttlSeconds = DEFAULT_TTL_SECONDS,
                    data = ByteArrayOutputStream().apply { writeDnsName(target) }.toByteArray(),
                )
            }

            fun srv(
                name: String,
                target: String,
                port: Int,
            ): ResourceRecord {
                return ResourceRecord(
                    name = name,
                    type = TYPE_SRV,
                    dnsClass = CLASS_IN_UNIQUE,
                    ttlSeconds = DEFAULT_TTL_SECONDS,
                    data = ByteArrayOutputStream().apply {
                        writeShort(0)
                        writeShort(0)
                        writeShort(port)
                        writeDnsName(target)
                    }.toByteArray(),
                )
            }

            fun txt(
                name: String,
                records: Map<String, String>,
            ): ResourceRecord {
                val data = ByteArrayOutputStream().apply {
                    records.forEach { (key, value) ->
                        val entry = "$key=$value".toByteArray(Charsets.UTF_8)
                        if (entry.size <= MAX_TXT_ENTRY_BYTES) {
                            write(entry.size)
                            write(entry)
                        }
                    }
                }.toByteArray()

                return ResourceRecord(
                    name = name,
                    type = TYPE_TXT,
                    dnsClass = CLASS_IN_UNIQUE,
                    ttlSeconds = DEFAULT_TTL_SECONDS,
                    data = data,
                )
            }

            fun a(
                name: String,
                address: Inet4Address,
            ): ResourceRecord {
                return ResourceRecord(
                    name = name,
                    type = TYPE_A,
                    dnsClass = CLASS_IN_UNIQUE,
                    ttlSeconds = DEFAULT_TTL_SECONDS,
                    data = address.address,
                )
            }
        }
    }

    private data class MdnsQuery(
        val questions: List<MdnsQuestion>,
    ) {
        companion object {
            fun parse(bytes: ByteArray): MdnsQuery? {
                if (bytes.size < DNS_HEADER_BYTES) {
                    return null
                }

                val questionCount = bytes.readUnsignedShort(4)
                var offset = DNS_HEADER_BYTES
                val questions = mutableListOf<MdnsQuestion>()

                repeat(questionCount) {
                    val decoded = readName(bytes, offset) ?: return null
                    offset = decoded.nextOffset
                    if (offset + QUESTION_TRAILER_BYTES > bytes.size) {
                        return null
                    }
                    val type = bytes.readUnsignedShort(offset)
                    val dnsClass = bytes.readUnsignedShort(offset + 2)
                    offset += QUESTION_TRAILER_BYTES
                    questions += MdnsQuestion(
                        name = decoded.name,
                        type = type,
                        dnsClass = dnsClass,
                    )
                }

                return MdnsQuery(questions)
            }

            private fun readName(
                bytes: ByteArray,
                startOffset: Int,
            ): DecodedName? {
                val labels = mutableListOf<String>()
                var offset = startOffset
                var nextOffset = startOffset
                var jumped = false
                var safety = 0

                while (offset < bytes.size && safety++ < MAX_NAME_PARTS) {
                    val length = bytes[offset].toInt() and BYTE_MASK
                    when {
                        length == 0 -> {
                            if (!jumped) {
                                nextOffset = offset + 1
                            }
                            return DecodedName(labels.joinToString(".") + ".", nextOffset)
                        }

                        length and POINTER_MASK == POINTER_MASK -> {
                            if (offset + 1 >= bytes.size) {
                                return null
                            }
                            val pointer = ((length and POINTER_VALUE_MASK) shl BYTE_BITS) or
                                (bytes[offset + 1].toInt() and BYTE_MASK)
                            if (!jumped) {
                                nextOffset = offset + 2
                            }
                            offset = pointer
                            jumped = true
                        }

                        else -> {
                            val labelStart = offset + 1
                            val labelEnd = labelStart + length
                            if (labelEnd > bytes.size) {
                                return null
                            }
                            labels += bytes.copyOfRange(labelStart, labelEnd).toString(Charsets.UTF_8)
                            offset = labelEnd
                            if (!jumped) {
                                nextOffset = offset
                            }
                        }
                    }
                }

                return null
            }
        }
    }

    private data class MdnsQuestion(
        val name: String,
        val type: Int,
        val dnsClass: Int,
    )

    private data class DecodedName(
        val name: String,
        val nextOffset: Int,
    )

    private companion object {
        const val BYTE_BITS = 8
        const val BYTE_MASK = 0xFF
        const val CLASS_IN = 0x0001
        const val CLASS_IN_UNIQUE = 0x8001
        const val DEFAULT_TTL_SECONDS = 120
        const val DNS_HEADER_BYTES = 12
        const val IPP_SERVICE_TYPE = "_ipp._tcp.local."
        const val USCAN_SERVICE_TYPE = "_uscan._tcp.local."
        const val USCANS_SERVICE_TYPE = "_uscans._tcp.local."
        const val LOCAL_DOMAIN = "local."
        const val MAX_NAME_PARTS = 64
        const val MAX_TXT_ENTRY_BYTES = 255
        const val POINTER_MASK = 0xC0
        const val POINTER_VALUE_MASK = 0x3F
        const val QUESTION_TRAILER_BYTES = 4
        const val RESPONSE_FLAGS = 0x8400
        const val TYPE_A = 0x0001
        const val TYPE_PTR = 0x000C
        const val TYPE_SRV = 0x0021
        const val TYPE_TXT = 0x0010
        const val UNIVERSAL_SUBTYPE = "_universal._sub._ipp._tcp.local."
    }
}

private fun ByteArrayOutputStream.writeDnsName(name: String) {
    name.trimEnd('.')
        .split(".")
        .filter(String::isNotBlank)
        .forEach { label ->
            val labelBytes = label.toByteArray(Charsets.UTF_8)
            write(labelBytes.size)
            write(labelBytes)
        }
    write(0)
}

private fun ByteArrayOutputStream.writeInt(value: Int) {
    write(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).array())
}

private fun ByteArrayOutputStream.writeShort(value: Int) {
    write(ByteBuffer.allocate(Short.SIZE_BYTES).putShort(value.toShort()).array())
}

private fun ByteArray.readUnsignedShort(offset: Int): Int {
    return ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)
}

private fun String.normalizeDnsName(): String {
    return trimEnd('.').lowercase(Locale.US)
}

private fun String.sanitizeDnsLabel(): String {
    return replace(Regex("[^A-Za-z0-9-]"), "-")
        .trim('-')
        .ifBlank { "airprint" }
}
