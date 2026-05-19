package com.archura.airprint.infrastructure.airprint.mdns

interface DnsServiceListener {
    fun onRegistered(service: BonjourService)

    fun onRegistrationError(message: String)
}
