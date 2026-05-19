package com.archura.airprint.infrastructure.airprint.mdns

data class BonjourService(
    val name: String,
    val type: String,
    val port: Int,
)
