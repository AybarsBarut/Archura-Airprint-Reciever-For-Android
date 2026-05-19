package com.archura.airprint.infrastructure.airprint.ipp

import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.archura.airprint.infrastructure.airprint.airscan.AirScanProtocolHandler

@Singleton
class IppPrintServer @Inject constructor(
    private val ippProtocolHandler: IppProtocolHandler,
    private val airScanProtocolHandler: AirScanProtocolHandler,
) {
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var sslServerSocket: ServerSocket? = null

    fun start(
        port: Int,
        scope: CoroutineScope,
    ) {
        if (serverJob?.isActive == true) {
            return
        }

        serverJob = scope.launch(Dispatchers.IO) {
            val securePort = port + 1

            // Start plain HTTP Server (on port)
            try {
                serverSocket = ServerSocket(port)
                launch {
                    while (isActive) {
                        val socket = serverSocket?.accept() ?: break
                        launch {
                            handleClient(socket)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start plain HTTP Server on port $port", e)
            }

            // Start HTTPS Server (on securePort)
            try {
                sslServerSocket = createSSLServerSocket(securePort)
                launch {
                    while (isActive) {
                        val socket = sslServerSocket?.accept() ?: break
                        launch {
                            handleClient(socket)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start HTTPS SSL Server on port $securePort", e)
            }
        }
    }

    private fun createSSLServerSocket(port: Int): ServerSocket {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "airscan_ssl_key"
        if (!keyStore.containsAlias(alias)) {
            val kpg = java.security.KeyPairGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_RSA,
                "AndroidKeyStore"
            )
            val start = java.util.Calendar.getInstance()
            val end = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, 5) }
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                alias,
                android.security.keystore.KeyProperties.PURPOSE_SIGN or android.security.keystore.KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(2048)
                .setCertificateSubject(javax.security.auth.x500.X500Principal("CN=AirScanReceiver"))
                .setCertificateSerialNumber(java.math.BigInteger.ONE)
                .setCertificateNotBefore(start.time)
                .setCertificateNotAfter(end.time)
                .setSignaturePaddings(android.security.keystore.KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setDigests(android.security.keystore.KeyProperties.DIGEST_SHA256, android.security.keystore.KeyProperties.DIGEST_SHA512)
                .build()
            kpg.initialize(spec)
            kpg.generateKeyPair()
        }

        val kmf = javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, null)
        }
        val sslContext = javax.net.ssl.SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, null, null)
        }
        return sslContext.serverSocketFactory.createServerSocket(port)
    }

    fun stop() {
        serverSocket?.close()
        serverSocket = null
        sslServerSocket?.close()
        sslServerSocket = null
        serverJob?.cancel()
        serverJob = null
    }

    private suspend fun handleClient(socket: Socket) {
        withContext(Dispatchers.IO) {
            socket.use { clientSocket ->
                val request = readHttpRequest(clientSocket)
                val senderAddress = clientSocket.inetAddress?.hostAddress
                Log.i(
                    TAG,
                    "IPP request from $senderAddress: " +
                        "headers=${request.headers.lineSequence().firstOrNull()} " +
                        "body=${request.body.size} bytes",
                )
                val requestLine = request.headers.lineSequence().firstOrNull() ?: ""
                val path = requestLine.split(" ").getOrNull(1) ?: "/"

                if (path.startsWith("/eSCL/")) {
                    Log.i(TAG, "AirScan request from $senderAddress: $requestLine")
                    val response = airScanProtocolHandler.handle(request.headers, request.body)
                    clientSocket.getOutputStream().writeAirScanResponse(response)
                } else {
                    val ippResponse = runCatching {
                        ippProtocolHandler.handle(request.body, senderAddress)
                    }.getOrElse { error ->
                        Log.e(TAG, "IPP request failed", error)
                        IppResponseBuilder().buildBadRequestResponse(requestId = 1)
                    }

                    clientSocket.getOutputStream().writeHttpResponse(ippResponse)
                }
            }
        }
    }

    private fun readHttpRequest(socket: Socket): HttpRequest {
        val input = socket.getInputStream()
        val headerBytes = ByteArrayOutputStream()
        var previousFour = 0

        while (true) {
            val next = input.read()
            if (next == END_OF_STREAM) {
                break
            }
            headerBytes.write(next)
            previousFour = ((previousFour shl BYTE_BITS) or next) and FOUR_BYTE_MASK
            if (previousFour == HEADER_TERMINATOR) {
                break
            }
        }

        val headers = headerBytes.toString(Charsets.ISO_8859_1.name())
        val transferEncoding = headers.lineSequence()
            .firstOrNull { line -> line.startsWith(TRANSFER_ENCODING_HEADER, ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            .orEmpty()
        val contentLength = headers.lineSequence()
            .firstOrNull { line -> line.startsWith(CONTENT_LENGTH_HEADER, ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.toIntOrNull()
            ?: 0

        val body = if (transferEncoding.equals(CHUNKED_ENCODING, ignoreCase = true)) {
            readChunkedBody(input)
        } else {
            readFixedLengthBody(input, contentLength)
        }

        return HttpRequest(
            headers = headers,
            body = body,
        )
    }

    private fun readFixedLengthBody(
        input: java.io.InputStream,
        contentLength: Int,
    ): ByteArray {
        if (contentLength <= 0) {
            return ByteArray(0)
        }

        val body = ByteArray(contentLength)
        var totalRead = 0
        while (totalRead < contentLength) {
            val read = input.read(body, totalRead, contentLength - totalRead)
            if (read == END_OF_STREAM) {
                break
            }
            totalRead += read
        }

        return if (totalRead == contentLength) body else body.copyOf(totalRead)
    }

    private fun readChunkedBody(input: java.io.InputStream): ByteArray {
        val body = ByteArrayOutputStream()

        while (true) {
            val chunkSizeLine = input.readAsciiLine()
            val chunkSize = chunkSizeLine
                .substringBefore(";")
                .trim()
                .toIntOrNull(radix = HEX_RADIX)
                ?: break

            if (chunkSize == 0) {
                input.readAsciiLine()
                break
            }

            val chunk = ByteArray(chunkSize)
            var totalRead = 0
            while (totalRead < chunkSize) {
                val read = input.read(chunk, totalRead, chunkSize - totalRead)
                if (read == END_OF_STREAM) {
                    break
                }
                totalRead += read
            }
            body.write(chunk, 0, totalRead)
            input.readAsciiLine()
        }

        return body.toByteArray()
    }

    private fun java.io.InputStream.readAsciiLine(): String {
        val line = ByteArrayOutputStream()
        while (true) {
            val next = read()
            if (next == END_OF_STREAM) {
                break
            }
            if (next == LINE_FEED) {
                break
            }
            if (next != CARRIAGE_RETURN) {
                line.write(next)
            }
        }
        return line.toString(Charsets.US_ASCII.name())
    }

    private fun java.io.OutputStream.writeHttpResponse(ippResponse: ByteArray) {
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: application/ipp\r\n")
            append("Content-Length: ${ippResponse.size}\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }.toByteArray(Charsets.ISO_8859_1)

        write(headers)
        write(ippResponse)
        flush()
    }

    private fun java.io.OutputStream.writeAirScanResponse(response: com.archura.airprint.infrastructure.airprint.airscan.AirScanResponse) {
        val statusText = when (response.statusCode) {
            200 -> "OK"
            201 -> "Created"
            else -> "Not Found"
        }
        val headers = buildString {
            append("HTTP/1.1 ${response.statusCode} $statusText\r\n")
            append("Content-Type: ${response.contentType}\r\n")
            append("Content-Length: ${response.body.size}\r\n")
            response.headers.forEach { (key, value) ->
                append("$key: $value\r\n")
            }
            append("Connection: close\r\n")
            append("\r\n")
        }.toByteArray(Charsets.ISO_8859_1)

        write(headers)
        if (response.body.isNotEmpty()) {
            write(response.body)
        }
        flush()
    }

    private data class HttpRequest(
        val headers: String,
        val body: ByteArray,
    )

    private companion object {
        const val BYTE_BITS = 8
        const val CARRIAGE_RETURN = 0x0D
        const val CHUNKED_ENCODING = "chunked"
        const val CONTENT_LENGTH_HEADER = "content-length"
        const val END_OF_STREAM = -1
        const val FOUR_BYTE_MASK = 0xFFFFFFFF.toInt()
        const val HEADER_TERMINATOR = 0x0D0A0D0A
        const val HEX_RADIX = 16
        const val LINE_FEED = 0x0A
        const val TAG = "IppPrintServer"
        const val TRANSFER_ENCODING_HEADER = "transfer-encoding"
    }
}
