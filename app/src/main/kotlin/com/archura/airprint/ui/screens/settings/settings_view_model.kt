package com.archura.airprint.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archura.airprint.data.local.SharedPrefManager
import com.archura.airprint.domain.model.DocumentConversionMode
import com.archura.airprint.domain.repository.PrinterStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val printerStatusRepository: PrinterStatusRepository,
    private val sharedPrefManager: SharedPrefManager,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(
        SettingsScreenState(
            printerStatus = printerStatusRepository.printerStatus.value,
            documentConversionMode = sharedPrefManager.readDocumentConversionMode(),
        ),
    )

    val uiState: StateFlow<SettingsScreenState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            printerStatusRepository.printerStatus.collect { printerStatus ->
                mutableUiState.update { state ->
                    state.copy(printerStatus = printerStatus)
                }
            }
        }
    }

    fun updateServiceName(serviceName: String) {
        viewModelScope.launch {
            printerStatusRepository.setServiceName(serviceName)
        }
    }

    fun updatePort(port: Int) {
        viewModelScope.launch {
            printerStatusRepository.setPort(port)
        }
    }

    fun updateDocumentConversionMode(mode: DocumentConversionMode) {
        sharedPrefManager.writeDocumentConversionMode(mode)
        mutableUiState.update { state ->
            state.copy(documentConversionMode = mode)
        }
    }
}
