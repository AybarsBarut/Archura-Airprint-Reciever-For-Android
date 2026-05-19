package com.archura.airprint.ui.screens.home

import com.archura.airprint.domain.model.PrinterStatus
import com.archura.airprint.domain.model.ReceivedImage

data class HomeScreenState(
    val images: List<ReceivedImage> = emptyList(),
    val printerStatus: PrinterStatus = PrinterStatus(),
    val errorMessage: String? = null,
)
