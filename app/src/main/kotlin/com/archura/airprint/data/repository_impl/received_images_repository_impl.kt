package com.archura.airprint.data.repository_impl

import com.archura.airprint.data.local.ReceivedImageLocalDataSource
import com.archura.airprint.data.local.SharedPrefManager
import com.archura.airprint.domain.model.DocumentConversionMode
import com.archura.airprint.domain.model.DocumentFormat
import com.archura.airprint.domain.model.ReceivedImage
import com.archura.airprint.domain.repository.ReceivedImagesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
class ReceivedImagesRepositoryImpl @Inject constructor(
    private val localDataSource: ReceivedImageLocalDataSource,
    private val sharedPrefManager: SharedPrefManager,
) : ReceivedImagesRepository {

    private val receivedImages = MutableStateFlow(localDataSource.listReceivedImages())

    override fun getReceivedImages(): Flow<List<ReceivedImage>> {
        return receivedImages.asStateFlow()
    }

    override suspend fun saveReceivedDocument(
        documentBytes: ByteArray,
        format: DocumentFormat,
        senderAddress: String?,
    ): ReceivedImage {
        return withContext(Dispatchers.IO) {
            val savedImage = when {
                shouldConvertPdfToImage(format) -> localDataSource.savePdfAllPagesAsImages(
                    documentBytes = documentBytes,
                    senderAddress = senderAddress,
                    format = if (sharedPrefManager.readDocumentConversionMode() == DocumentConversionMode.PDF_TO_PNG) DocumentFormat.PNG else DocumentFormat.JPEG
                )
                else -> localDataSource.saveReceivedDocument(
                    documentBytes = documentBytes,
                    format = format,
                    senderAddress = senderAddress,
                )
            }
            refresh()
            savedImage
        }
    }

    override suspend fun cropReceivedImage(imageId: String): ReceivedImage {
        return withContext(Dispatchers.IO) {
            val croppedImage = localDataSource.cropReceivedImage(imageId)
            refresh()
            croppedImage
        }
    }

    override suspend fun applyManualCrop(imageId: String, uri: android.net.Uri): ReceivedImage {
        return withContext(Dispatchers.IO) {
            val croppedImage = localDataSource.applyManualCrop(imageId, uri)
            refresh()
            croppedImage
        }
    }

    override suspend fun undoEdit(imageId: String): ReceivedImage {
        return withContext(Dispatchers.IO) {
            val restoredImage = localDataSource.undoEdit(imageId)
            refresh()
            restoredImage
        }
    }

    override suspend fun convertFormat(imageId: String, targetFormat: DocumentFormat): ReceivedImage {
        return withContext(Dispatchers.IO) {
            val convertedImage = localDataSource.convertFormat(imageId, targetFormat)
            refresh()
            convertedImage
        }
    }

    override suspend fun hasUndo(imageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            localDataSource.hasUndo(imageId)
        }
    }

    override suspend fun deleteReceivedImage(imageId: String) {
        withContext(Dispatchers.IO) {
            localDataSource.deleteReceivedImage(imageId)
            refresh()
        }
    }

    override suspend fun refresh() {
        receivedImages.value = localDataSource.listReceivedImages()
    }

    override suspend fun rotateReceivedImage(
        imageId: String,
        degrees: Float,
    ): ReceivedImage {
        return withContext(Dispatchers.IO) {
            val rotatedImage = localDataSource.rotateReceivedImage(
                imageId = imageId,
                degrees = degrees,
            )
            refresh()
            rotatedImage
        }
    }

    override suspend fun saveReceivedImageToGallery(imageId: String): String {
        return withContext(Dispatchers.IO) {
            localDataSource.saveReceivedImageToGallery(imageId)
        }
    }

    private fun shouldConvertPdfToImage(format: DocumentFormat): Boolean {
        val mode = sharedPrefManager.readDocumentConversionMode()
        return format == DocumentFormat.PDF &&
            (mode == DocumentConversionMode.PDF_FIRST_PAGE_TO_JPEG || mode == DocumentConversionMode.PDF_TO_PNG)
    }
}
