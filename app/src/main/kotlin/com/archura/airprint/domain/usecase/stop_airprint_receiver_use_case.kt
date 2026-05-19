package com.archura.airprint.domain.usecase

import com.archura.airprint.domain.repository.PrinterStatusRepository
import javax.inject.Inject

class StopAirPrintReceiverUseCase @Inject constructor(
    private val printerStatusRepository: PrinterStatusRepository,
) {
    suspend operator fun invoke() {
        printerStatusRepository.setEnabled(false)
    }
}
