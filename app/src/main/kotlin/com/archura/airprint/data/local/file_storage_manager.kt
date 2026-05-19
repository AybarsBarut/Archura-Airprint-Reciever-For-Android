package com.archura.airprint.data.local

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.archura.airprint.domain.model.DocumentFormat
import com.archura.airprint.domain.model.ReceivedImage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class FileStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun listReceivedImages(): List<ReceivedImage> {
        return receivedDirectory
            .listFiles()
            ?.filter { file -> file.isFile }
            ?.map { file -> file.toReceivedImage() }
            ?.sortedByDescending { image -> image.timestampMillis }
            .orEmpty()
    }

    fun saveDocument(
        documentBytes: ByteArray,
        format: DocumentFormat,
        senderAddress: String?,
    ): ReceivedImage {
        val now = System.currentTimeMillis()
        val fileName = "${now}_${UUID.randomUUID()}.${format.extension}"
        val targetFile = File(receivedDirectory, fileName)

        targetFile.writeBytes(documentBytes)

        return targetFile.toReceivedImage(
            timestampMillis = now,
            format = format,
            senderAddress = senderAddress,
        )
    }

    fun importDocument(uri: Uri): ReceivedImage? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: ""
        val format = when {
            mimeType.contains("pdf") || uri.path?.lowercase()?.endsWith(".pdf") == true -> DocumentFormat.PDF
            mimeType.contains("png") || uri.path?.lowercase()?.endsWith(".png") == true -> DocumentFormat.PNG
            mimeType.contains("jpeg") || mimeType.contains("jpg") || uri.path?.lowercase()?.endsWith(".jpg") == true || uri.path?.lowercase()?.endsWith(".jpeg") == true -> DocumentFormat.JPEG
            else -> DocumentFormat.JPEG
        }

        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        return saveDocument(bytes, format, "Local Device Import")
    }

    fun savePdfAllPagesAsImages(
        documentBytes: ByteArray,
        senderAddress: String?,
        format: DocumentFormat,
    ): List<ReceivedImage> {
        val now = System.currentTimeMillis()
        val tempPdfFile = File(cacheDirectory, "${now}_${UUID.randomUUID()}.pdf")
        tempPdfFile.writeBytes(documentBytes)

        return try {
            val generatedImages = mutableListOf<ReceivedImage>()
            renderPdfPagesToImages(
                pdfFile = tempPdfFile,
                timestampMillis = now,
                senderAddress = senderAddress,
                format = format,
                onImageGenerated = { generatedImages.add(it) }
            )
            generatedImages
        } finally {
            tempPdfFile.delete()
        }
    }

    fun undoEdit(imageId: String): ReceivedImage {
        val imageFile = findReceivedFile(imageId)
        val backupFile = File(imageFile.absolutePath + ".bak")
        require(backupFile.exists()) { "No backup available to undo" }
        backupFile.copyTo(imageFile, overwrite = true)
        imageFile.setLastModified(System.currentTimeMillis())
        backupFile.delete()
        return imageFile.toReceivedImage()
    }

    fun convertFormat(imageId: String, targetFormat: DocumentFormat): ReceivedImage {
        val oldFile = findReceivedFile(imageId)
        val oldFormat = oldFile.extension.toDocumentFormat()
        require(oldFormat != targetFormat) { "File is already in $targetFormat" }

        val newFileName = "${oldFile.nameWithoutExtension}.${targetFormat.extension}"
        val newFile = File(oldFile.parentFile, newFileName)

        if (oldFormat == DocumentFormat.PDF) {
            val pdfBytes = oldFile.readBytes()
            val generated = savePdfAllPagesAsImages(pdfBytes, null, targetFormat)
            if (generated.isNotEmpty()) {
                oldFile.delete()
                return generated.first()
            } else {
                error("Failed to render PDF pages")
            }
        } else if (oldFormat == DocumentFormat.JPEG || oldFormat == DocumentFormat.PNG) {
            val bitmap = decodeImage(oldFile)
            writeBitmap(bitmap, newFile)
            bitmap.recycle()
            oldFile.delete()
            File(oldFile.absolutePath + ".bak").delete()
            return newFile.toReceivedImage()
        } else {
            error("Unsupported format conversion from $oldFormat")
        }
    }

    fun hasUndo(imageId: String): Boolean {
        val imageFile = findReceivedFile(imageId)
        return File(imageFile.absolutePath + ".bak").exists()
    }

    fun applyManualCrop(imageId: String, croppedImageUri: Uri): ReceivedImage {
        val imageFile = findReceivedFile(imageId)
        createBackup(imageFile)
        val resolver = context.contentResolver
        resolver.openInputStream(croppedImageUri)?.use { input ->
            imageFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        imageFile.setLastModified(System.currentTimeMillis())
        return imageFile.toReceivedImage()
    }

    private fun createBackup(imageFile: File) {
        val backupFile = File(imageFile.absolutePath + ".bak")
        imageFile.copyTo(backupFile, overwrite = true)
    }

    fun cropCenterSquare(imageId: String): ReceivedImage {
        val imageFile = findReceivedFile(imageId)
        createBackup(imageFile)
        val bitmap = decodeImage(imageFile)
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, left, top, size, size)
        writeBitmap(cropped, imageFile)
        bitmap.recycle()
        cropped.recycle()
        imageFile.setLastModified(System.currentTimeMillis())
        return imageFile.toReceivedImage()
    }

    fun deleteById(imageId: String) {
        receivedDirectory
            .listFiles()
            ?.firstOrNull { file -> file.nameWithoutExtension == imageId }
            ?.let { file ->
                file.delete()
                File(file.absolutePath + ".bak").delete()
            }
    }

    fun rotateImage(
        imageId: String,
        degrees: Float,
    ): ReceivedImage {
        val imageFile = findReceivedFile(imageId)
        createBackup(imageFile)
        val bitmap = decodeImage(imageFile)
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        writeBitmap(rotated, imageFile)
        bitmap.recycle()
        rotated.recycle()
        imageFile.setLastModified(System.currentTimeMillis())
        return imageFile.toReceivedImage()
    }

    fun saveImageToGallery(imageId: String): String {
        val imageFile = findReceivedFile(imageId)
        require(imageFile.extension.toDocumentFormat() in listOf(DocumentFormat.JPEG, DocumentFormat.PNG)) {
            "Only JPEG/PNG images can be saved to gallery"
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGalleryWithMediaStore(imageFile).toString()
        } else {
            saveImageToLegacyGallery(imageFile).absolutePath
        }
    }

    fun saveDocumentToDownloads(imageId: String): String {
        val imageFile = findReceivedFile(imageId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveDocumentToDownloadsWithMediaStore(imageFile).toString()
        } else {
            saveDocumentToLegacyDownloads(imageFile).absolutePath
        }
    }

    private fun saveDocumentToDownloadsWithMediaStore(imageFile: File): Uri {
        val format = imageFile.extension.toDocumentFormat()
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, imageFile.name)
            put(MediaStore.Downloads.MIME_TYPE, format.mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = requireNotNull(
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
        ) {
            "Unable to create downloads document"
        }

        resolver.openOutputStream(uri).use { output ->
            requireNotNull(output) {
                "Unable to open downloads output stream"
            }
            imageFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun saveDocumentToLegacyDownloads(imageFile: File): File {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val targetFile = File(directory, imageFile.name)
        imageFile.copyTo(targetFile, overwrite = true)
        val format = imageFile.extension.toDocumentFormat()
        MediaScannerConnection.scanFile(
            context,
            arrayOf(targetFile.absolutePath),
            arrayOf(format.mimeType),
            null,
        )
        return targetFile
    }

    private val receivedDirectory: File
        get() {
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val directory = picturesDir ?: File(context.filesDir, RECEIVED_IMAGES_DIRECTORY)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            return directory
        }

    private val cacheDirectory: File
        get() {
            val directory = File(context.cacheDir, PDF_RENDER_CACHE_DIRECTORY)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            return directory
        }

    private fun decodeImage(imageFile: File): Bitmap {
        require(imageFile.extension.toDocumentFormat() in listOf(DocumentFormat.JPEG, DocumentFormat.PNG)) {
            "Only JPEG/PNG images can be edited"
        }
        return requireNotNull(android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)) {
            "Unable to decode image"
        }
    }

    private fun findReceivedFile(imageId: String): File {
        return receivedDirectory
            .listFiles()
            ?.firstOrNull { file -> file.nameWithoutExtension == imageId }
            ?: error("Received image not found: $imageId")
    }

    private fun renderPdfPagesToImages(
        pdfFile: File,
        timestampMillis: Long,
        senderAddress: String?,
        format: DocumentFormat,
        onImageGenerated: (ReceivedImage) -> Unit,
    ) {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                require(renderer.pageCount > 0) {
                    "PDF has no pages"
                }

                for (pageIndex in 0 until renderer.pageCount) {
                    renderer.openPage(pageIndex).use { page ->
                        val bitmap = page.createBitmap()
                        val targetFile = File(receivedDirectory, "${timestampMillis}_${UUID.randomUUID()}_page${pageIndex + 1}.${format.extension}")
                        
                        renderPageToBitmap(page, bitmap)
                        writeBitmap(bitmap, targetFile)
                        bitmap.recycle()
                        
                        onImageGenerated(
                            targetFile.toReceivedImage(
                                timestampMillis = timestampMillis + pageIndex, // offset by page index to sort properly
                                format = format,
                                senderAddress = senderAddress,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun PdfRenderer.Page.createBitmap(): Bitmap {
        // Scale by 3 to improve PDF rendering quality
        val scale = 3
        return Bitmap.createBitmap(
            (width * scale).coerceAtLeast(MIN_RENDER_SIZE),
            (height * scale).coerceAtLeast(MIN_RENDER_SIZE),
            Bitmap.Config.ARGB_8888,
        )
    }

    private fun renderPageToBitmap(
        page: PdfRenderer.Page,
        bitmap: Bitmap,
    ) {
        bitmap.eraseColor(Color.WHITE)
        page.render(
            bitmap,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
        )
    }

    private fun writeBitmap(
        bitmap: Bitmap,
        targetFile: File,
    ) {
        val compressFormat = if (targetFile.extension.lowercase() == "png") {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
        FileOutputStream(targetFile).use { output ->
            bitmap.compress(compressFormat, 100, output)
        }
    }

    private fun saveImageToGalleryWithMediaStore(imageFile: File): Uri {
        val format = imageFile.extension.toDocumentFormat()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "$PICTURES_DIRECTORY/$GALLERY_DIRECTORY")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = requireNotNull(
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
        ) {
            "Unable to create gallery image"
        }

        resolver.openOutputStream(uri).use { output ->
            requireNotNull(output) {
                "Unable to open gallery output stream"
            }
            imageFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun saveImageToLegacyGallery(imageFile: File): File {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            GALLERY_DIRECTORY,
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val targetFile = File(directory, imageFile.name)
        imageFile.copyTo(targetFile, overwrite = true)
        val format = imageFile.extension.toDocumentFormat()
        MediaScannerConnection.scanFile(
            context,
            arrayOf(targetFile.absolutePath),
            arrayOf(format.mimeType),
            null,
        )
        return targetFile
    }

    private fun File.toReceivedImage(
        timestampMillis: Long = lastModified(),
        format: DocumentFormat = extension.toDocumentFormat(),
        senderAddress: String? = null,
    ): ReceivedImage {
        return ReceivedImage(
            id = nameWithoutExtension,
            path = absolutePath,
            fileName = name,
            timestampMillis = timestampMillis,
            format = format,
            senderAddress = senderAddress,
            sizeBytes = length(),
        )
    }

    private fun String.toDocumentFormat(): DocumentFormat {
        return when (lowercase()) {
            DocumentFormat.JPEG.extension -> DocumentFormat.JPEG
            DocumentFormat.PNG.extension -> DocumentFormat.PNG
            DocumentFormat.TXT.extension -> DocumentFormat.TXT
            DocumentFormat.PDF.extension -> DocumentFormat.PDF
            DocumentFormat.URF.extension -> DocumentFormat.URF
            DocumentFormat.PWG_RASTER.extension -> DocumentFormat.PWG_RASTER
            else -> DocumentFormat.UNKNOWN
        }
    }

    private companion object {
        const val FIRST_PAGE_INDEX = 0
        const val GALLERY_DIRECTORY = "AirPrint Receiver"
        const val JPEG_QUALITY = 92
        const val MIN_RENDER_SIZE = 1
        const val PDF_RENDER_CACHE_DIRECTORY = "pdf_render_cache"
        const val PICTURES_DIRECTORY = "Pictures"
        const val RECEIVED_IMAGES_DIRECTORY = "received_images"
    }
}
