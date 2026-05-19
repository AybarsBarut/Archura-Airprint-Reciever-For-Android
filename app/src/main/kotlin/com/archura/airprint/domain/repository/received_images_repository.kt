package com.archura.airprint.domain.repository

import com.archura.airprint.domain.model.DocumentFormat
import com.archura.airprint.domain.model.ReceivedImage
import kotlinx.coroutines.flow.Flow

interface ReceivedImagesRepository {
    fun getReceivedImages(): Flow<List<ReceivedImage>>

    suspend fun saveReceivedDocument(
        documentBytes: ByteArray,
        format: DocumentFormat,
        senderAddress: String?,
    ): ReceivedImage

    suspend fun cropReceivedImage(imageId: String): ReceivedImage

    suspend fun applyManualCrop(imageId: String, uri: android.net.Uri): ReceivedImage

    suspend fun undoEdit(imageId: String): ReceivedImage

    suspend fun hasUndo(imageId: String): Boolean

    suspend fun deleteReceivedImage(imageId: String)

    suspend fun refresh()

    suspend fun rotateReceivedImage(
        imageId: String,
        degrees: Float,
    ): ReceivedImage

    suspend fun saveReceivedImageToGallery(imageId: String): String
}
