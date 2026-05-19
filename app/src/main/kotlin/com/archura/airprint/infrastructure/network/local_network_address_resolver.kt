package com.archura.airprint.infrastructure.network

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import javax.inject.Inject

class LocalNetworkAddressResolver @Inject constructor() {
    fun resolveIpv4Address(): Inet4Address? {
        return interfaces()
            .flatMap { networkInterface ->
                Collections.list(networkInterface.inetAddresses).asSequence()
            }
            .filterIsInstance<Inet4Address>()
            .filter { address ->
                !address.isLoopbackAddress &&
                    !address.isLinkLocalAddress &&
                    address.isSiteLocalAddress
            }
            .firstOrNull()
    }

    fun resolveInterface(address: Inet4Address): NetworkInterface? {
        return NetworkInterface.getByInetAddress(address)
            ?.takeIf { networkInterface ->
                networkInterface.isUp &&
                    !networkInterface.isLoopback &&
                    networkInterface.supportsMulticast()
            }
    }

    private fun interfaces(): Sequence<NetworkInterface> {
        return Collections.list(NetworkInterface.getNetworkInterfaces())
            .asSequence()
            .filter { networkInterface ->
                networkInterface.isUp &&
                    !networkInterface.isLoopback &&
                    networkInterface.supportsMulticast()
            }
    }
}
