package com.archura.airprint.ui.screens.settings

import com.archura.airprint.domain.model.DocumentConversionMode
import com.archura.airprint.domain.model.PrinterStatus

data class SettingsScreenState(
    val printerStatus: PrinterStatus = PrinterStatus(),
    val documentConversionMode: DocumentConversionMode = DocumentConversionMode.KEEP_ORIGINAL,
)
