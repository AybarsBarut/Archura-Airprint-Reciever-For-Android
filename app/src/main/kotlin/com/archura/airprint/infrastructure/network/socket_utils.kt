package com.archura.airprint.infrastructure.network

import java.net.ServerSocket

object SocketUtils {
    fun findFreePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }
}
