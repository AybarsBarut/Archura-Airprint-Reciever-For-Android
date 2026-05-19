package com.archura.airprint.infrastructure.airprint.airscan

import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.util.Log
import java.io.ByteArrayOutputStream

private const val TAG = "ImageToPdfConverter"

object ImageToPdfConverter {

    /**
     * Converts raw image bytes (JPEG, PNG, etc.) into a valid single-page PDF document.
     * If conversion fails, returns the original bytes.
     */
    fun convert(imageBytes: ByteArray): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) {
                Log.w(TAG, "Failed to decode image bytes into bitmap, returning original bytes")
                return imageBytes
            }

            val pdfDocument = PdfDocument()
            // Standard A4 size in points (1 point = 1/72 inch) at 72 DPI is 595 x 842.
            // We can either use the image dimensions or standard A4.
            // Let's use the image's original dimensions so we don't stretch/distort it.
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            val outputStream = ByteArrayOutputStream()
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            bitmap.recycle()

            val pdfBytes = outputStream.toByteArray()
            Log.i(TAG, "Successfully converted image to PDF: ${imageBytes.size} bytes -> ${pdfBytes.size} bytes")
            pdfBytes
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to PDF", e)
            imageBytes
        }
    }
}
