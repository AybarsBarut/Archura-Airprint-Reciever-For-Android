package com.archura.airprint.data.local

import com.archura.airprint.domain.model.DocumentFormat
import com.archura.airprint.domain.model.ReceivedImage
import javax.inject.Inject

class ReceivedImageLocalDataSource @Inject constructor(
    private val fileStorageManager: FileStorageManager,
) {
    fun listReceivedImages(): List<ReceivedImage> {
        return fileStorageManager.listReceivedImages()
    }

    fun saveReceivedDocument(
        documentBytes: ByteArray,
        format: DocumentFormat,
        senderAddress: String?,
    ): ReceivedImage {
        return fileStorageManager.saveDocument(
            documentBytes = documentBytes,
            format = format,
            senderAddress = senderAddress,
        )
    }

    fun savePdfFirstPageAsJpeg(
        documentBytes: ByteArray,
        senderAddress: String?,
    ): ReceivedImage {
        return fileStorageManager.savePdfFirstPageAsJpeg(
            documentBytes = documentBytes,
            senderAddress = senderAddress,
        )
    }

    fun cropReceivedImage(imageId: String): ReceivedImage {
        return fileStorageManager.cropCenterSquare(imageId)
    }

    fun deleteReceivedImage(imageId: String) {
        fileStorageManager.deleteById(imageId)
    }

    fun rotateReceivedImage(
        imageId: String,
        degrees: Float,
    ): ReceivedImage {
        return fileStorageManager.rotateImage(
            imageId = imageId,
            degrees = degrees,
        )
    }

    fun saveReceivedImageToGallery(imageId: String): String {
        return fileStorageManager.saveImageToGallery(imageId)
    }
}
