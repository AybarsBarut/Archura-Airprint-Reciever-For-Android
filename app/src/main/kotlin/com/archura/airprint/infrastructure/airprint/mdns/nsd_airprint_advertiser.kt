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
    private var uscanRegistrationListener: NsdManager.RegistrationListener? = null

    fun register(
        serviceName: String,
        port: Int,
        onStatusChanged: (registered: Boolean, error: String?) -> Unit,
    ) {
        unregister()

        val sanitizedServiceName = serviceName.replace(Regex("[^A-Za-z0-9-]"), "-").trim('-').ifBlank { "airprint" }
        val sanitizedUscanServiceName = (serviceName + " Scanner").replace(Regex("[^A-Za-z0-9-]"), "-").trim('-').ifBlank { "airprint" }

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = sanitizedServiceName
            serviceType = SERVICE_TYPE
            this.port = port
            AirPrintTxtRecords.build(
                serviceName = sanitizedServiceName,
                uuid = airPrintDeviceIdentity.uuid,
                localAddress = null,
                port = port,
            ).forEach { (key, value) ->
                setAttribute(key, value)
            }
        }

        val uscanServiceInfo = NsdServiceInfo().apply {
            this.serviceName = sanitizedUscanServiceName
            serviceType = USCAN_SERVICE_TYPE
            this.port = port
            AirScanTxtRecords.build(
                serviceName = sanitizedUscanServiceName,
                uuid = airPrintDeviceIdentity.uuid,
                localAddress = null,
                port = port,
            ).forEach { (key, value) ->
                setAttribute(key, value)
            }
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredServiceInfo: NsdServiceInfo) {
                android.util.Log.i("NsdAirPrintAdvertiser", "IPP service registered successfully: ${registeredServiceInfo.serviceName}")
                onStatusChanged(true, null)
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                android.util.Log.e("NsdAirPrintAdvertiser", "IPP service registration failed: $errorCode")
                onStatusChanged(false, "mDNS registration failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                android.util.Log.i("NsdAirPrintAdvertiser", "IPP service unregistered")
                onStatusChanged(false, "mDNS service unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                android.util.Log.e("NsdAirPrintAdvertiser", "IPP service unregistration failed: $errorCode")
                onStatusChanged(false, "mDNS unregister failed: $errorCode")
            }
        }

        val uscanListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredServiceInfo: NsdServiceInfo) {
                android.util.Log.i("NsdAirPrintAdvertiser", "AirScan service registered successfully: ${registeredServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                android.util.Log.e("NsdAirPrintAdvertiser", "AirScan service registration failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                android.util.Log.i("NsdAirPrintAdvertiser", "AirScan service unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                android.util.Log.e("NsdAirPrintAdvertiser", "AirScan service unregistration failed: $errorCode")
            }
        }

        registrationListener = listener
        uscanRegistrationListener = uscanListener
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        nsdManager.registerService(uscanServiceInfo, NsdManager.PROTOCOL_DNS_SD, uscanListener)
    }

    fun unregister() {
        registrationListener?.let { listener ->
            runCatching {
                nsdManager.unregisterService(listener)
            }
        }
        uscanRegistrationListener?.let { listener ->
            runCatching {
                nsdManager.unregisterService(listener)
            }
        }
        registrationListener = null
        uscanRegistrationListener = null
    }

    private companion object {
        const val SERVICE_TYPE = "_ipp._tcp."
        const val USCAN_SERVICE_TYPE = "_uscan._tcp."
    }
}
