package com.archura.airprint.ui.screens.received_images

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archura.airprint.domain.repository.ReceivedImagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReceivedImageDetailViewModel @Inject constructor(
    private val receivedImagesRepository: ReceivedImagesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val imageId = requireNotNull(savedStateHandle.get<String>(IMAGE_ID_ARG))
    private val mutableUiState = MutableStateFlow(ReceivedImageDetailState())

    val uiState: StateFlow<ReceivedImageDetailState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            receivedImagesRepository.getReceivedImages().collect { images ->
                val image = images.firstOrNull { it.id == imageId }
                val hasUndo = if (image != null) receivedImagesRepository.hasUndo(imageId) else false
                mutableUiState.update { state ->
                    state.copy(image = image, hasUndo = hasUndo)
                }
            }
        }
    }

    fun cropSquare() {
        runImageOperation(successMessage = "Image cropped") {
            receivedImagesRepository.cropReceivedImage(imageId)
        }
    }

    fun applyManualCrop(uri: android.net.Uri) {
        runImageOperation(successMessage = "Image manually cropped") {
            receivedImagesRepository.applyManualCrop(imageId, uri)
        }
    }

    fun undoEdit() {
        runImageOperation(successMessage = "Edit reverted") {
            receivedImagesRepository.undoEdit(imageId)
        }
    }

    fun rotateLeft() {
        runImageOperation(successMessage = "Image rotated left") {
            receivedImagesRepository.rotateReceivedImage(imageId, ROTATE_LEFT_DEGREES)
        }
    }

    fun rotateRight() {
        runImageOperation(successMessage = "Image rotated right") {
            receivedImagesRepository.rotateReceivedImage(imageId, ROTATE_RIGHT_DEGREES)
        }
    }

    fun saveToGallery() {
        runImageOperation(successMessage = "Saved to gallery") {
            receivedImagesRepository.saveReceivedImageToGallery(imageId)
        }
    }

    private fun runImageOperation(
        successMessage: String,
        operation: suspend () -> Any,
    ) {
        viewModelScope.launch {
            mutableUiState.update { state -> state.copy(isBusy = true, message = null) }
            val result = runCatching { operation() }
            val hasUndo = receivedImagesRepository.hasUndo(imageId)
            mutableUiState.update { state ->
                state.copy(
                    isBusy = false,
                    hasUndo = hasUndo,
                    message = result.fold(
                        onSuccess = { successMessage },
                        onFailure = { error -> error.message ?: "Image action failed" },
                    ),
                )
            }
        }
    }

    private companion object {
        const val IMAGE_ID_ARG = "imageId"
        const val ROTATE_LEFT_DEGREES = -90f
        const val ROTATE_RIGHT_DEGREES = 90f
    }
}
