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

    fun savePdfAllPagesAsImages(
        documentBytes: ByteArray,
        senderAddress: String?,
        format: DocumentFormat,
    ): ReceivedImage {
        val images = fileStorageManager.savePdfAllPagesAsImages(
            documentBytes = documentBytes,
            senderAddress = senderAddress,
            format = format,
        )
        return images.first()
    }

    fun cropReceivedImage(imageId: String): ReceivedImage {
        return fileStorageManager.cropCenterSquare(imageId)
    }

    fun applyManualCrop(imageId: String, uri: android.net.Uri): ReceivedImage {
        return fileStorageManager.applyManualCrop(imageId, uri)
    }

    fun undoEdit(imageId: String): ReceivedImage {
        return fileStorageManager.undoEdit(imageId)
    }

    fun convertFormat(imageId: String, targetFormat: DocumentFormat): ReceivedImage {
        return fileStorageManager.convertFormat(imageId, targetFormat)
    }

    fun hasUndo(imageId: String): Boolean {
        return fileStorageManager.hasUndo(imageId)
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

    fun saveReceivedDocumentToDownloads(imageId: String): String {
        return fileStorageManager.saveDocumentToDownloads(imageId)
    }
}
