package com.archura.airprint.ui.screens.home

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archura.airprint.domain.repository.PrinterStatusRepository
import com.archura.airprint.domain.repository.ReceivedImagesRepository
import com.archura.airprint.infrastructure.airprint.AirPrintServiceController
import com.archura.airprint.infrastructure.airprint.airscan.ScanRequestManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val receivedImagesRepository: ReceivedImagesRepository,
    private val printerStatusRepository: PrinterStatusRepository,
    private val airPrintServiceController: AirPrintServiceController,
    val scanRequestManager: ScanRequestManager,
) : ViewModel() {

    val uiState: StateFlow<HomeScreenState> = combine(
        receivedImagesRepository.getReceivedImages(),
        printerStatusRepository.printerStatus,
    ) { images, printerStatus ->
        HomeScreenState(
            images = images,
            printerStatus = printerStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
        initialValue = HomeScreenState(),
    )

    init {
        viewModelScope.launch {
            if (printerStatusRepository.printerStatus.value.enabled) {
                airPrintServiceController.start()
            }
        }
    }

    fun setReceiverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            printerStatusRepository.setEnabled(enabled)
            if (enabled) {
                airPrintServiceController.start()
            } else {
                airPrintServiceController.stop()
            }
        }
    }

    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            receivedImagesRepository.deleteReceivedImage(imageId)
        }
    }

    /** User picked a file via the manual "Share File with iOS" button. */
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            receivedImagesRepository.importDocument(uri)
        }
    }

    /**
     * User picked a file in response to an iOS scan request.
     * Reads the file bytes and fulfills the pending scan job immediately.
     */
    fun fulfillScanRequest(jobId: String, uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                    val contentType = when {
                        mimeType.contains("pdf") -> "application/pdf"
                        mimeType.contains("png") -> "image/png"
                        else -> "image/jpeg"
                    }
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        scanRequestManager.fulfillJob(jobId, bytes, contentType)
                        Log.i(TAG, "Fulfilled scan job $jobId with ${bytes.size} bytes ($contentType)")
                    } else {
                        Log.w(TAG, "File was empty or unreadable for job $jobId")
                        scanRequestManager.cancelJob(jobId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading file for scan job $jobId", e)
                    scanRequestManager.cancelJob(jobId)
                }
            }
        }
    }

    /** User dismissed the scan file picker without selecting a file. */
    fun cancelScanRequest(jobId: String) {
        scanRequestManager.cancelJob(jobId)
    }

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L
    }
}
