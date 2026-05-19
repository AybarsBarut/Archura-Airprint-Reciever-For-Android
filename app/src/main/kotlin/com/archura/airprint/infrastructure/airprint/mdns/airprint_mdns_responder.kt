package com.archura.airprint.infrastructure.airprint.mdns

import android.content.Context
import android.net.wifi.WifiManager
import com.archura.airprint.infrastructure.network.LocalNetworkAddressResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class AirPrintMdnsResponder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val airPrintDeviceIdentity: AirPrintDeviceIdentity,
    private val localNetworkAddressResolver: LocalNetworkAddressResolver,
) {
    private var responderJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var multicastSocket: MulticastSocket? = null

    fun start(
        serviceName: String,
        port: Int,
        scope: CoroutineScope,
        onStatusChanged: (started: Boolean, message: String) -> Unit,
    ) {
        if (responderJob?.isActive == true) {
            return
        }

        val localAddress = localNetworkAddressResolver.resolveIpv4Address()
        if (localAddress == null) {
            onStatusChanged(false, "No local IPv4 address for AirPrint mDNS")
            return
        }

        val networkInterface = localNetworkAddressResolver.resolveInterface(localAddress)
        if (networkInterface == null) {
            onStatusChanged(false, "No multicast-capable network interface")
            return
        }

        val packetBuilder = AirPrintMdnsPacket(
            serviceName = serviceName,
            hostName = airPrintDeviceIdentity.hostName(),
            port = port,
            localAddress = localAddress,
            txtRecords = AirPrintTxtRecords.build(
                serviceName = serviceName,
                uuid = airPrintDeviceIdentity.uuid,
                localAddress = localAddress.hostAddress,
                port = port,
            ),
        )

        acquireMulticastLock()
        responderJob = scope.launch(Dispatchers.IO) {
            runCatching {
                MulticastSocket(null).use { socket ->
                    multicastSocket = socket
                    socket.reuseAddress = true
                    socket.soTimeout = SOCKET_TIMEOUT_MILLIS
                    socket.bind(InetSocketAddress(MDNS_PORT))
                    socket.joinGroup(InetSocketAddress(MDNS_GROUP, MDNS_PORT), networkInterface)
                    onStatusChanged(true, "AirPrint mDNS responder on ${localAddress.hostAddress}:$port")
                    announce(socket, packetBuilder)
                    listen(socket, packetBuilder)
                }
            }.onFailure { error ->
                onStatusChanged(false, "AirPrint mDNS failed: ${error.message}")
            }
        }
    }

    fun stop() {
        responderJob?.cancel()
        responderJob = null
        multicastSocket?.close()
        multicastSocket = null
        multicastLock?.release()
        multicastLock = null
    }

    private suspend fun announce(
        socket: MulticastSocket,
        packetBuilder: AirPrintMdnsPacket,
    ) {
        repeat(ANNOUNCEMENT_COUNT) {
            sendMulticast(socket, packetBuilder.buildAnnouncement())
            delay(ANNOUNCEMENT_DELAY_MILLIS)
        }
    }

    private fun listen(
        socket: MulticastSocket,
        packetBuilder: AirPrintMdnsPacket,
    ) {
        val buffer = ByteArray(RECEIVE_BUFFER_BYTES)
        while (responderJob?.isActive == true) {
            val packet = DatagramPacket(buffer, buffer.size)
            val received = runCatching {
                socket.receive(packet)
                packet.data.copyOfRange(0, packet.length)
            }.getOrNull()

            val response = received?.let(packetBuilder::buildResponseIfRelevant)
            if (response != null) {
                sendMulticast(socket, response)
                sendUnicast(socket, response, packet)
            }
        }
    }

    private fun sendMulticast(
        socket: MulticastSocket,
        bytes: ByteArray,
    ) {
        val packet = DatagramPacket(bytes, bytes.size, MDNS_GROUP, MDNS_PORT)
        socket.send(packet)
    }

    private fun sendUnicast(
        socket: MulticastSocket,
        bytes: ByteArray,
        requestPacket: DatagramPacket,
    ) {
        val packet = DatagramPacket(
            bytes,
            bytes.size,
            requestPacket.address,
            requestPacket.port,
        )
        socket.send(packet)
    }

    private fun acquireMulticastLock() {
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        multicastLock = wifiManager.createMulticastLock(MULTICAST_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private companion object {
        const val ANNOUNCEMENT_COUNT = 3
        const val ANNOUNCEMENT_DELAY_MILLIS = 250L
        const val MDNS_PORT = 5353
        const val MULTICAST_LOCK_TAG = "airprint_mdns"
        const val RECEIVE_BUFFER_BYTES = 9_000
        const val SOCKET_TIMEOUT_MILLIS = 1_000
        val MDNS_GROUP: InetAddress = InetAddress.getByName("224.0.0.251")
    }
}
