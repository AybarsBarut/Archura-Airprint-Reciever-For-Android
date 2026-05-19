package com.archura.airprint.infrastructure.airprint

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirPrintServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun start() {
        val intent = Intent(context, AirPrintReceiverService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop() {
        val intent = Intent(context, AirPrintReceiverService::class.java)
        context.stopService(intent)
    }
}
