package com.archura.airprint.infrastructure.airprint.mdns

object AirScanTxtRecords {
    fun build(
        serviceName: String,
        uuid: String,
        localAddress: String?,
        port: Int,
        scheme: String = "http",
    ): Map<String, String> {
        val records = linkedMapOf(
            "txtvers" to "1",
            "ty" to serviceName,
            "note" to "AirScan Android",
            "vers" to "2.5",
            "rs" to "eSCL",
            "pdl" to "application/octet-stream,image/jpeg,application/pdf",
            "cs" to "binary,grayscale,color",
            "is" to "platen",
            "duplex" to "F",
            "UUID" to uuid,
            "uuid" to uuid,
        )

        if (localAddress != null) {
            records["adminurl"] = "$scheme://$localAddress%3A$port/"
        }

        return records
    }
}
