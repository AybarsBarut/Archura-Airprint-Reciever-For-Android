package com.archura.airprint.infrastructure.airprint.airscan

import android.util.Log
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ScanRequestManager"

data class ScanDocument(val bytes: ByteArray, val contentType: String)

@Singleton
class ScanRequestManager @Inject constructor() {

    /** Exposed to UI: non-null when a scan job is waiting for a file to be selected. */
    private val _pendingJobId = MutableStateFlow<String?>(null)
    val pendingJobId: StateFlow<String?> = _pendingJobId.asStateFlow()

    /** Jobs waiting for a document (not yet fulfilled). */
    private val waitingJobs = ConcurrentHashMap<String, Boolean>()

    /** Jobs that have a document ready to serve. */
    private val readyDocuments = ConcurrentHashMap<String, ScanDocument>()

    /** Jobs where the document was already delivered once → signal 404 next call. */
    private val deliveredJobs = ConcurrentHashMap<String, Boolean>()

    /** Called by AirScanProtocolHandler when iOS creates a scan job. */
    fun createJob(): String {
        val jobId = UUID.randomUUID().toString()
        waitingJobs[jobId] = true
        _pendingJobId.value = jobId
        Log.i(TAG, "Scan job created: $jobId — waiting for user to select a file")
        return jobId
    }

    /** Called by HomeViewModel when the user picks a file. */
    fun fulfillJob(jobId: String, bytes: ByteArray, contentType: String) {
        waitingJobs.remove(jobId)
        readyDocuments[jobId] = ScanDocument(bytes, contentType)
        if (_pendingJobId.value == jobId) {
            _pendingJobId.value = null
        }
        Log.i(TAG, "Job $jobId fulfilled: ${bytes.size} bytes ($contentType)")
    }

    /** Called by HomeViewModel if the user cancels the file picker. */
    fun cancelJob(jobId: String) {
        waitingJobs.remove(jobId)
        readyDocuments.remove(jobId)
        if (_pendingJobId.value == jobId) {
            _pendingJobId.value = null
        }
        Log.i(TAG, "Job $jobId cancelled by user")
    }

    /** Returns null if still waiting, the document if ready. */
    fun getDocument(jobId: String): ScanDocument? = readyDocuments[jobId]

    fun isWaiting(jobId: String): Boolean = waitingJobs.containsKey(jobId)

    fun isDelivered(jobId: String): Boolean = deliveredJobs.containsKey(jobId)

    fun markDelivered(jobId: String) {
        readyDocuments.remove(jobId)
        deliveredJobs[jobId] = true
        Log.i(TAG, "Job $jobId marked delivered")
    }

    fun cleanupJob(jobId: String) {
        waitingJobs.remove(jobId)
        readyDocuments.remove(jobId)
        deliveredJobs.remove(jobId)
    }
}
