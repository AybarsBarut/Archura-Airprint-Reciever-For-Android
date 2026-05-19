package com.archura.airprint.infrastructure.airprint.airscan

import android.util.Log
import com.archura.airprint.data.local.FileStorageManager
import com.archura.airprint.domain.model.DocumentFormat
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AirScanHandler"

@Singleton
class AirScanProtocolHandler @Inject constructor(
    private val fileStorageManager: FileStorageManager,
) {
    // jobId → "pending" | "delivered"
    private val jobs = ConcurrentHashMap<String, String>()

    fun handle(headers: String, body: ByteArray): AirScanResponse {
        val requestLine = headers.lineSequence().firstOrNull() ?: ""
        val parts = requestLine.split(" ")
        val method = parts.getOrNull(0) ?: "GET"
        val path = parts.getOrNull(1) ?: "/"
        val cleanPath = path.substringBefore("?").removeSuffix("/")

        Log.i(TAG, "$method $cleanPath")

        return when {
            method == "GET" && cleanPath.endsWith("/ScannerCapabilities") -> getScannerCapabilities()
            method == "GET" && cleanPath.endsWith("/ScannerStatus") -> getScannerStatus()
            method == "POST" && cleanPath.endsWith("/ScanJobs") -> createScanJob()
            method == "GET" && cleanPath.contains("/ScanJobs/") && cleanPath.endsWith("/NextDocument") -> {
                val jobId = cleanPath.substringAfterLast("/ScanJobs/").substringBefore("/NextDocument")
                getNextDocument(jobId)
            }
            else -> AirScanResponse(404, "text/plain", "Not Found".toByteArray())
        }
    }

    private fun getScannerCapabilities(): AirScanResponse {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<scan:ScannerCapabilities
    xmlns:scan="http://schemas.hp.com/imaging/escl/2011/05/03"
    xmlns:pwg="http://www.pwg.org/schemas/2010/12/sm">
  <pwg:Version>2.63</pwg:Version>
  <pwg:MakeAndModel>Archura AirScan Android</pwg:MakeAndModel>
  <pwg:SerialNumber>ARCHURA-ANDROID-001</pwg:SerialNumber>
  <scan:UUID>archura-android-airscan-001</scan:UUID>
  <scan:AdminURI></scan:AdminURI>
  <scan:IconURI></scan:IconURI>
  <scan:Platen>
    <scan:PlatenInputCaps>
      <scan:MinWidth>1</scan:MinWidth>
      <scan:MaxWidth>2550</scan:MaxWidth>
      <scan:MinHeight>1</scan:MinHeight>
      <scan:MaxHeight>3507</scan:MaxHeight>
      <scan:MaxPhysicalWidth>2550</scan:MaxPhysicalWidth>
      <scan:MaxPhysicalHeight>3507</scan:MaxPhysicalHeight>
      <scan:MaxScanRegions>1</scan:MaxScanRegions>
      <scan:SettingProfiles>
        <scan:SettingProfile>
          <scan:ColorModes>
            <scan:ColorMode>RGB24</scan:ColorMode>
            <scan:ColorMode>Grayscale8</scan:ColorMode>
          </scan:ColorModes>
          <scan:DocumentFormats>
            <pwg:DocumentFormat>image/jpeg</pwg:DocumentFormat>
            <pwg:DocumentFormat>image/png</pwg:DocumentFormat>
            <pwg:DocumentFormat>application/pdf</pwg:DocumentFormat>
            <scan:DocumentFormatExt>image/jpeg</scan:DocumentFormatExt>
            <scan:DocumentFormatExt>image/png</scan:DocumentFormatExt>
            <scan:DocumentFormatExt>application/pdf</scan:DocumentFormatExt>
          </scan:DocumentFormats>
          <scan:SupportedResolutions>
            <scan:DiscreteResolutions>
              <scan:DiscreteResolution>
                <scan:XResolution>75</scan:XResolution>
                <scan:YResolution>75</scan:YResolution>
              </scan:DiscreteResolution>
              <scan:DiscreteResolution>
                <scan:XResolution>150</scan:XResolution>
                <scan:YResolution>150</scan:YResolution>
              </scan:DiscreteResolution>
              <scan:DiscreteResolution>
                <scan:XResolution>300</scan:XResolution>
                <scan:YResolution>300</scan:YResolution>
              </scan:DiscreteResolution>
              <scan:DiscreteResolution>
                <scan:XResolution>600</scan:XResolution>
                <scan:YResolution>600</scan:YResolution>
              </scan:DiscreteResolution>
            </scan:DiscreteResolutions>
          </scan:SupportedResolutions>
          <scan:ColorSpaces>
            <scan:ColorSpace>YCC</scan:ColorSpace>
            <scan:ColorSpace>sRGB</scan:ColorSpace>
          </scan:ColorSpaces>
        </scan:SettingProfile>
      </scan:SettingProfiles>
      <scan:ScanIntents>
        <scan:ScanIntent>Document</scan:ScanIntent>
        <scan:ScanIntent>Photo</scan:ScanIntent>
        <scan:ScanIntent>TextAndGraphic</scan:ScanIntent>
      </scan:ScanIntents>
    </scan:PlatenInputCaps>
  </scan:Platen>
</scan:ScannerCapabilities>""".trimIndent()

        return AirScanResponse(200, "text/xml; charset=UTF-8", xml.toByteArray(Charsets.UTF_8))
    }

    private fun getScannerStatus(): AirScanResponse {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<scan:ScannerStatus
    xmlns:scan="http://schemas.hp.com/imaging/escl/2011/05/03"
    xmlns:pwg="http://www.pwg.org/schemas/2010/12/sm">
  <pwg:Version>2.63</pwg:Version>
  <pwg:State>Idle</pwg:State>
  <scan:Jobs/>
</scan:ScannerStatus>""".trimIndent()

        return AirScanResponse(200, "text/xml; charset=UTF-8", xml.toByteArray(Charsets.UTF_8))
    }

    private fun createScanJob(): AirScanResponse {
        val jobId = UUID.randomUUID().toString()
        jobs[jobId] = "pending"
        Log.i(TAG, "Created scan job: $jobId (total pending: ${jobs.size})")
        return AirScanResponse(
            statusCode = 201,
            contentType = "text/plain",
            body = ByteArray(0),
            headers = mapOf("Location" to "/eSCL/ScanJobs/$jobId"),
        )
    }

    private fun getNextDocument(jobId: String): AirScanResponse {
        val state = jobs[jobId]
        Log.i(TAG, "NextDocument for job $jobId — state=$state")

        if (state == "delivered") {
            // Already delivered once — signal end-of-job
            jobs.remove(jobId)
            Log.i(TAG, "Job $jobId completed")
            return AirScanResponse(404, "text/plain", "No more documents".toByteArray())
        }

        // Pick the most recently imported file
        val allFiles = fileStorageManager.listReceivedImages().filter { image ->
            image.format == DocumentFormat.JPEG ||
                image.format == DocumentFormat.PNG ||
                image.format == DocumentFormat.PDF
        }

        val target = allFiles.firstOrNull()
        if (target == null) {
            Log.w(TAG, "No file available to serve for job $jobId")
            return AirScanResponse(404, "text/plain", "No documents available to scan".toByteArray())
        }

        val file = File(target.path)
        if (!file.exists()) {
            Log.w(TAG, "File missing: ${target.path}")
            return AirScanResponse(404, "text/plain", "File not found".toByteArray())
        }

        val contentType = when (target.format) {
            DocumentFormat.PNG -> "image/png"
            DocumentFormat.PDF -> "application/pdf"
            else -> "image/jpeg"
        }

        val bytes = file.readBytes()
        Log.i(TAG, "Serving ${file.name} (${bytes.size} bytes, $contentType) for job $jobId")

        // Mark as delivered so next call returns 404 (end-of-job)
        jobs[jobId] = "delivered"

        return AirScanResponse(200, contentType, bytes)
    }
}

data class AirScanResponse(
    val statusCode: Int,
    val contentType: String,
    val body: ByteArray,
    val headers: Map<String, String> = emptyMap(),
)
