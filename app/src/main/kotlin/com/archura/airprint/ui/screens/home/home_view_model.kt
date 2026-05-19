package com.archura.airprint.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archura.airprint.domain.repository.PrinterStatusRepository
import com.archura.airprint.domain.repository.ReceivedImagesRepository
import com.archura.airprint.infrastructure.airprint.AirPrintServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val receivedImagesRepository: ReceivedImagesRepository,
    private val printerStatusRepository: PrinterStatusRepository,
    private val airPrintServiceController: AirPrintServiceController,
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

    fun importFile(uri: android.net.Uri) {
        viewModelScope.launch {
            receivedImagesRepository.importDocument(uri)
        }
    }

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L
    }
}
