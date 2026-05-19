package com.archura.airprint.domain.repository

import com.archura.airprint.domain.model.PrinterStatus
import kotlinx.coroutines.flow.StateFlow

interface PrinterStatusRepository {
    val printerStatus: StateFlow<PrinterStatus>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setPort(port: Int)

    suspend fun setServiceName(serviceName: String)

    suspend fun setStatusMessage(message: String?)
}
