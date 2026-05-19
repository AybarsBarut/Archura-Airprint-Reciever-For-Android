package com.archura.airprint.infrastructure.airprint.mdns

object AirPrintTxtRecords {
    fun build(
        serviceName: String,
        uuid: String,
        localAddress: String?,
        port: Int,
    ): Map<String, String> {
        val records = linkedMapOf(
            "txtvers" to "1",
            "qtotal" to "1",
            "rp" to "ipp/print",
            "ty" to serviceName,
            "product" to "(Archura AirPrint Receiver)",
            "pdl" to "application/pdf,image/jpeg,image/urf,image/pwg-raster",
            "URF" to "CP1,IS1-4-5,MT1-2-3-4-5-6,RS600,V1.4,W8,SRGB24",
            "Color" to "T",
            "Duplex" to "F",
            "Scan" to "T",
            "Fax" to "F",
            "Binary" to "T",
            "Transparent" to "T",
            "air" to "none",
            "kind" to "document,envelope,label,postcard",
            "UUID" to uuid,
        )

        if (localAddress != null) {
            records["adminurl"] = "http://$localAddress:$port/"
        }

        return records
    }
}
