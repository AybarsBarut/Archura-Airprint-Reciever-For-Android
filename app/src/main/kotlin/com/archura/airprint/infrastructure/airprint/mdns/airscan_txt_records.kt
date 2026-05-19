package com.archura.airprint.infrastructure.airprint.mdns

object AirScanTxtRecords {
    fun build(serviceName: String, uuid: String, port: Int): Map<String, String> {
        return mapOf(
            "txtvers" to "1",
            "ty" to serviceName,
            "adminurl" to "http://local:$port/",
            "pdl" to "application/pdf,image/jpeg",
            "cs" to "color,grayscale",
            "rs" to "eSCL",
            "UUID" to uuid,
            "representation" to "http://local:$port/icon.png",
        )
    }
}
