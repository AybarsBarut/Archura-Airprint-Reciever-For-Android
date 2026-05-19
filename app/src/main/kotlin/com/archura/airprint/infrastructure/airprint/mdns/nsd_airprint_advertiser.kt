package com.archura.airprint.infrastructure.airprint.mdns

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsdAirPrintAdvertiser @Inject constructor(
    @ApplicationContext context: Context,
    private val airPrintDeviceIdentity: AirPrintDeviceIdentity,
) {
    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun register(
        serviceName: String,
        port: Int,
        onStatusChanged: (registered: Boolean, error: String?) -> Unit,
    ) {
        unregister()

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            serviceType = SERVICE_TYPE
            this.port = port
            AirPrintTxtRecords.build(
                serviceName = serviceName,
                uuid = airPrintDeviceIdentity.uuid,
                localAddress = null,
                port = port,
            ).forEach { (key, value) ->
                setAttribute(key, value)
            }
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredServiceInfo: NsdServiceInfo) {
                onStatusChanged(true, null)
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                onStatusChanged(false, "mDNS registration failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                onStatusChanged(false, "mDNS service unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                onStatusChanged(false, "mDNS unregister failed: $errorCode")
            }
        }

        registrationListener = listener
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun unregister() {
        registrationListener?.let { listener ->
            runCatching {
                nsdManager.unregisterService(listener)
            }
        }
        registrationListener = null
    }

    private companion object {
        const val SERVICE_TYPE = "_ipp._tcp."
    }
}
