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
                shouldConvertPdfToJpeg(format) -> localDataSource.savePdfFirstPageAsJpeg(
                    documentBytes = documentBytes,
                    senderAddress = senderAddress,
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

    private fun shouldConvertPdfToJpeg(format: DocumentFormat): Boolean {
        return format == DocumentFormat.PDF &&
            sharedPrefManager.readDocumentConversionMode() == DocumentConversionMode.PDF_FIRST_PAGE_TO_JPEG
    }
}
