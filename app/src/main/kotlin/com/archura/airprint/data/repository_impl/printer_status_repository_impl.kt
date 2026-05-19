package com.archura.airprint.data.repository_impl

import com.archura.airprint.data.local.SharedPrefManager
import com.archura.airprint.domain.repository.PrinterStatusRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PrinterStatusRepositoryImpl @Inject constructor(
    private val sharedPrefManager: SharedPrefManager,
) : PrinterStatusRepository {

    private val mutablePrinterStatus = MutableStateFlow(sharedPrefManager.readPrinterStatus())

    override val printerStatus: StateFlow<com.archura.airprint.domain.model.PrinterStatus> =
        mutablePrinterStatus.asStateFlow()

    override suspend fun setEnabled(enabled: Boolean) {
        updateStatus { status -> status.copy(enabled = enabled) }
    }

    override suspend fun setPort(port: Int) {
        updateStatus { status -> status.copy(port = port.coerceIn(MIN_PORT, MAX_PORT)) }
    }

    override suspend fun setServiceName(serviceName: String) {
        updateStatus { status -> status.copy(serviceName = serviceName.trim()) }
    }

    override suspend fun setStatusMessage(message: String?) {
        updateStatus { status -> status.copy(statusMessage = message) }
    }

    private fun updateStatus(
        transform: (com.archura.airprint.domain.model.PrinterStatus) -> com.archura.airprint.domain.model.PrinterStatus,
    ) {
        val updated = transform(mutablePrinterStatus.value)
        mutablePrinterStatus.value = updated
        sharedPrefManager.writePrinterStatus(updated)
    }

    private companion object {
        const val MAX_PORT = 65_535
        const val MIN_PORT = 1_024
    }
}
