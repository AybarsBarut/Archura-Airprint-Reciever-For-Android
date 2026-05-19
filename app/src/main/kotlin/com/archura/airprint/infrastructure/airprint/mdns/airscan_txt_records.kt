package com.archura.airprint.infrastructure.airprint.mdns

object AirScanTxtRecords {
    fun build(serviceName: String, uuid: String, port: Int): Map<String, String> {
        return mapOf(
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
            "adminurl" to "http://localhost:$port/",
        )
    }
}
