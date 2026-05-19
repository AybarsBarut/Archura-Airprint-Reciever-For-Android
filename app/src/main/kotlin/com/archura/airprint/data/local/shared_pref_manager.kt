package com.archura.airprint.data.local

import android.content.Context
import com.archura.airprint.domain.model.DocumentConversionMode
import com.archura.airprint.domain.model.PrinterStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readPrinterStatus(): PrinterStatus {
        return PrinterStatus(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            serviceName = preferences.getString(KEY_SERVICE_NAME, PrinterStatus.DEFAULT_SERVICE_NAME)
                ?: PrinterStatus.DEFAULT_SERVICE_NAME,
            port = preferences.getInt(KEY_PORT, PrinterStatus.DEFAULT_IPP_PORT),
            statusMessage = null,
        )
    }

    fun writePrinterStatus(printerStatus: PrinterStatus) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, printerStatus.enabled)
            .putString(KEY_SERVICE_NAME, printerStatus.serviceName)
            .putInt(KEY_PORT, printerStatus.port)
            .apply()
    }

    fun readDocumentConversionMode(): DocumentConversionMode {
        return DocumentConversionMode.fromStoredValue(
            preferences.getString(KEY_DOCUMENT_CONVERSION_MODE, null),
        )
    }

    fun writeDocumentConversionMode(mode: DocumentConversionMode) {
        preferences.edit()
            .putString(KEY_DOCUMENT_CONVERSION_MODE, mode.name)
            .apply()
    }

    private companion object {
        const val KEY_DOCUMENT_CONVERSION_MODE = "document_conversion_mode"
        const val KEY_ENABLED = "enabled"
        const val KEY_PORT = "port"
        const val KEY_SERVICE_NAME = "service_name"
        const val PREFS_NAME = "airprint_receiver_settings"
    }
}
