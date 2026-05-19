package com.archura.airprint.infrastructure.airprint.airscan

import com.archura.airprint.data.local.FileStorageManager
import com.archura.airprint.domain.model.DocumentFormat
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirScanProtocolHandler @Inject constructor(
    private val fileStorageManager: FileStorageManager
) {

    fun handle(headers: String, body: ByteArray): AirScanResponse {
        val requestLine = headers.lineSequence().firstOrNull() ?: ""
        val parts = requestLine.split(" ")
        val method = parts.getOrNull(0) ?: "GET"
        val path = parts.getOrNull(1) ?: "/"

        val cleanPath = path.substringBefore("?").removeSuffix("/")

        return when {
            method == "GET" && cleanPath.endsWith("/ScannerCapabilities") -> getScannerCapabilities()
            method == "GET" && cleanPath.endsWith("/ScannerStatus") -> getScannerStatus()
            method == "POST" && cleanPath.endsWith("/ScanJobs") -> createScanJob()
            method == "GET" && cleanPath.contains("/ScanJobs/") && cleanPath.endsWith("/NextDocument") -> getNextDocument()
            else -> AirScanResponse(404, "text/plain", "Not Found".toByteArray())
        }
    }

    private fun getScannerCapabilities(): AirScanResponse {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<scan:ScannerCapabilities xmlns:scan="http://schemas.hp.com/imaging/escl/2011/05/03" xmlns:pwg="http://www.pwg.org/schemas/2010/12/sm">
  <pwg:Version>2.6</pwg:Version>
  <pwg:MakeAndModel>AirPrint Android Scanner</pwg:MakeAndModel>
  <pwg:SerialNumber>123456</pwg:SerialNumber>
  <scan:Intent>Document</scan:Intent>
  <scan:Modifier>
    <scan:Resolution>
      <pwg:XResolution>300</pwg:XResolution>
      <pwg:YResolution>300</pwg:YResolution>
    </scan:Resolution>
  </scan:Modifier>
  <scan:ColorModes>
    <scan:ColorMode>RGB24</scan:ColorMode>
    <scan:ColorMode>Grayscale8</scan:ColorMode>
  </scan:ColorModes>
</scan:ScannerCapabilities>""".trimIndent()

        return AirScanResponse(200, "text/xml", xml.toByteArray())
    }

    private fun getScannerStatus(): AirScanResponse {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<scan:ScannerStatus xmlns:scan="http://schemas.hp.com/imaging/escl/2011/05/03" xmlns:pwg="http://www.pwg.org/schemas/2010/12/sm">
  <pwg:Version>2.6</pwg:Version>
  <scan:State>Idle</scan:State>
</scan:ScannerStatus>""".trimIndent()

        return AirScanResponse(200, "text/xml", xml.toByteArray())
    }

    private fun createScanJob(): AirScanResponse {
        return AirScanResponse(
            statusCode = 201,
            contentType = "text/plain",
            body = ByteArray(0),
            headers = mapOf("Location" to "/eSCL/ScanJobs/1")
        )
    }

    private fun getNextDocument(): AirScanResponse {
        val images = fileStorageManager.listReceivedImages().filter { 
            it.format == DocumentFormat.JPEG || it.format == DocumentFormat.PNG 
        }
        val latestImage = images.firstOrNull()
        
        if (latestImage != null) {
            val file = File(latestImage.path)
            if (file.exists()) {
                val contentType = if (latestImage.format == DocumentFormat.PNG) "image/png" else "image/jpeg"
                return AirScanResponse(200, contentType, file.readBytes())
            }
        }
        
        // Return 404 if no image available to scan
        return AirScanResponse(404, "text/plain", "No documents available to scan".toByteArray())
    }
}

data class AirScanResponse(
    val statusCode: Int,
    val contentType: String,
    val body: ByteArray,
    val headers: Map<String, String> = emptyMap()
)
