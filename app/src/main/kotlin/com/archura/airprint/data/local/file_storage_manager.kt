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

    fun savePdfFirstPageAsJpeg(
        documentBytes: ByteArray,
        senderAddress: String?,
    ): ReceivedImage {
        val now = System.currentTimeMillis()
        val tempPdfFile = File(cacheDirectory, "${now}_${UUID.randomUUID()}.pdf")
        tempPdfFile.writeBytes(documentBytes)

        return try {
            val jpegFile = File(receivedDirectory, "${now}_${UUID.randomUUID()}.${DocumentFormat.JPEG.extension}")
            renderPdfFirstPageToJpeg(
                pdfFile = tempPdfFile,
                targetFile = jpegFile,
            )
            jpegFile.toReceivedImage(
                timestampMillis = now,
                format = DocumentFormat.JPEG,
                senderAddress = senderAddress,
            )
        } finally {
            tempPdfFile.delete()
        }
    }

    fun cropCenterSquare(imageId: String): ReceivedImage {
        val imageFile = findReceivedFile(imageId)
        val bitmap = decodeJpeg(imageFile)
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, left, top, size, size)
        writeBitmapAsJpeg(cropped, imageFile)
        bitmap.recycle()
        cropped.recycle()
        imageFile.setLastModified(System.currentTimeMillis())
        return imageFile.toReceivedImage()
    }

    fun deleteById(imageId: String) {
        receivedDirectory
            .listFiles()
            ?.firstOrNull { file -> file.nameWithoutExtension == imageId }
            ?.delete()
    }

    fun rotateImage(
        imageId: String,
        degrees: Float,
    ): ReceivedImage {
        val imageFile = findReceivedFile(imageId)
        val bitmap = decodeJpeg(imageFile)
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        writeBitmapAsJpeg(rotated, imageFile)
        bitmap.recycle()
        rotated.recycle()
        imageFile.setLastModified(System.currentTimeMillis())
        return imageFile.toReceivedImage()
    }

    fun saveImageToGallery(imageId: String): String {
        val imageFile = findReceivedFile(imageId)
        require(imageFile.extension.toDocumentFormat() == DocumentFormat.JPEG) {
            "Only JPEG images can be saved to gallery"
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGalleryWithMediaStore(imageFile).toString()
        } else {
            saveImageToLegacyGallery(imageFile).absolutePath
        }
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

    private fun decodeJpeg(imageFile: File): Bitmap {
        require(imageFile.extension.toDocumentFormat() == DocumentFormat.JPEG) {
            "Only JPEG images can be edited"
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

    private fun renderPdfFirstPageToJpeg(
        pdfFile: File,
        targetFile: File,
    ) {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderFirstPage(renderer, targetFile)
            }
        }
    }

    private fun renderFirstPage(
        renderer: PdfRenderer,
        targetFile: File,
    ) {
        require(renderer.pageCount > 0) {
            "PDF has no pages"
        }

        renderer.openPage(FIRST_PAGE_INDEX).use { page ->
            val bitmap = page.createBitmap()
            renderPageToBitmap(page, bitmap)
            writeBitmapAsJpeg(bitmap, targetFile)
            bitmap.recycle()
        }
    }

    private fun PdfRenderer.Page.createBitmap(): Bitmap {
        return Bitmap.createBitmap(
            width.coerceAtLeast(MIN_RENDER_SIZE),
            height.coerceAtLeast(MIN_RENDER_SIZE),
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

    private fun writeBitmapAsJpeg(
        bitmap: Bitmap,
        targetFile: File,
    ) {
        FileOutputStream(targetFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        }
    }

    private fun saveImageToGalleryWithMediaStore(imageFile: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
            put(MediaStore.Images.Media.MIME_TYPE, DocumentFormat.JPEG.mimeType)
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
        MediaScannerConnection.scanFile(
            context,
            arrayOf(targetFile.absolutePath),
            arrayOf(DocumentFormat.JPEG.mimeType),
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
