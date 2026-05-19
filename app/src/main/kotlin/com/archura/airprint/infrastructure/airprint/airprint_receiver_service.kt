package com.archura.airprint.infrastructure.airprint

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.archura.airprint.R
import com.archura.airprint.domain.repository.PrinterStatusRepository
import com.archura.airprint.infrastructure.airprint.ipp.IppPrintServer
import com.archura.airprint.infrastructure.airprint.mdns.AirPrintMdnsResponder
import com.archura.airprint.infrastructure.airprint.mdns.NsdAirPrintAdvertiser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AirPrintReceiverService : Service() {

    @Inject lateinit var ippPrintServer: IppPrintServer
    @Inject lateinit var airPrintMdnsResponder: AirPrintMdnsResponder
    @Inject lateinit var nsdAirPrintAdvertiser: NsdAirPrintAdvertiser
    @Inject lateinit var printerStatusRepository: PrinterStatusRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startReceiver()
        return START_STICKY
    }

    override fun onDestroy() {
        airPrintMdnsResponder.stop()
        nsdAirPrintAdvertiser.unregister()
        ippPrintServer.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startReceiver() {
        serviceScope.launch {
            val status = printerStatusRepository.printerStatus.value
            ippPrintServer.start(
                port = status.port,
                scope = serviceScope,
            )
            airPrintMdnsResponder.start(
                serviceName = status.serviceName,
                port = status.port,
                scope = serviceScope,
            ) { _, message ->
                serviceScope.launch {
                    printerStatusRepository.setStatusMessage(message)
                }
            }
            nsdAirPrintAdvertiser.register(
                serviceName = status.serviceName,
                port = status.port,
            ) { registered, error ->
                serviceScope.launch {
                    printerStatusRepository.setStatusMessage(
                        message = if (registered) {
                            "Advertising ${status.serviceName} on port ${status.port}"
                        } else {
                            error ?: "Unable to advertise AirPrint service"
                        },
                    )
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.airprint_service_notification))
            .setSmallIcon(R.drawable.ic_stat_print)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.airprint_service_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "airprint_receiver"
        const val NOTIFICATION_ID = 9010
    }
}
