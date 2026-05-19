package com.archura.airprint.domain.model

data class PrinterStatus(
    val enabled: Boolean = false,
    val serviceName: String = DEFAULT_SERVICE_NAME,
    val port: Int = DEFAULT_IPP_PORT,
    val statusMessage: String? = null,
) {
    companion object {
        const val DEFAULT_IPP_PORT = 9100
        const val DEFAULT_SERVICE_NAME = "Archura AirPrint Receiver"
    }
}
