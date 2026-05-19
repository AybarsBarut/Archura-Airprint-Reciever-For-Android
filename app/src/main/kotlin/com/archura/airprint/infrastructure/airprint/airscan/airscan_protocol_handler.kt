package com.archura.airprint.infrastructure.airprint.airscan

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AirScanHandler"

@Singleton
class AirScanProtocolHandler @Inject constructor(
    private val scanRequestManager: ScanRequestManager,
) {

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
        val jobId = scanRequestManager.createJob()
        return AirScanResponse(
            statusCode = 201,
            contentType = "text/plain",
            body = ByteArray(0),
            headers = mapOf("Location" to "/eSCL/ScanJobs/$jobId"),
        )
    }

    private fun getNextDocument(jobId: String): AirScanResponse {
        // Already delivered → signal end-of-job
        if (scanRequestManager.isDelivered(jobId)) {
            scanRequestManager.cleanupJob(jobId)
            Log.i(TAG, "Job $jobId: already delivered → 404")
            return AirScanResponse(404, "text/plain", "No more documents".toByteArray())
        }

        // Still waiting for user to pick a file → 503 (SwiftESCL retries indefinitely)
        if (scanRequestManager.isWaiting(jobId)) {
            Log.i(TAG, "Job $jobId: waiting for file selection → 503")
            return AirScanResponse(503, "text/plain", "Scanner warming up".toByteArray())
        }

        // Document is ready — serve it
        val doc = scanRequestManager.getDocument(jobId)
        if (doc == null) {
            Log.w(TAG, "Job $jobId: no document found → 404")
            return AirScanResponse(404, "text/plain", "No document available".toByteArray())
        }

        Log.i(TAG, "Job $jobId: serving ${doc.bytes.size} bytes (${doc.contentType})")
        scanRequestManager.markDelivered(jobId)
        return AirScanResponse(200, doc.contentType, doc.bytes)
    }
}

data class AirScanResponse(
    val statusCode: Int,
    val contentType: String,
    val body: ByteArray,
    val headers: Map<String, String> = emptyMap(),
)
