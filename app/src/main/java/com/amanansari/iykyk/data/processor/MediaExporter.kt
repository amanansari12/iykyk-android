package com.amanansari.iykyk.data.processor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

/**
 * Turns the finished collage bitmap into something the OS can use —
 * a permanent gallery entry, or a disposable shareable file.
 */
class MediaExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun saveToGallery(bitmap: Bitmap): Uri? {
        val fileName = "iykyk_collage_${System.currentTimeMillis()}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToGalleryQAndAbove(bitmap, fileName)
        } else {
            saveToGalleryLegacy(bitmap, fileName)
        }
    }

    private fun saveToGalleryQAndAbove(bitmap: Bitmap, fileName: String): Uri? {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/IYKYK")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: run {
                Log.e("MediaExporter", "MediaStore insert returned null Uri")
                return null
            }

        return try {
            resolver.openOutputStream(itemUri)?.use { out -> writeBitmap(bitmap, out) }
                ?: throw IOException("openOutputStream returned null")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)

            Log.d("MediaExporter", "Saved collage to gallery: $itemUri")
            itemUri
        } catch (e: Exception) {
            Log.e("MediaExporter", "Failed saving collage to gallery", e)
            resolver.delete(itemUri, null, null)
            null
        }
    }

    private fun saveToGalleryLegacy(bitmap: Bitmap, fileName: String): Uri? {
        return try {
            val picturesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "IYKYK"
            )
            if (!picturesDir.exists()) picturesDir.mkdirs()

            val file = File(picturesDir, fileName)
            file.outputStream().use { out -> writeBitmap(bitmap, out) }

            MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null
            )

            Log.d("MediaExporter", "Saved collage to gallery (legacy): ${file.absolutePath}")
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("MediaExporter", "Failed saving collage to gallery (legacy)", e)
            null
        }
    }

    /** Cache-dir copy for handing to another app via ACTION_SEND. Not a permanent save. */
    fun getShareableUri(bitmap: Bitmap): Uri? {
        return try {
            val shareDir = File(context.cacheDir, "shared_collages")
            if (!shareDir.exists()) shareDir.mkdirs()

            val file = File(shareDir, "collage_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> writeBitmap(bitmap, out) }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("MediaExporter", "Failed preparing shareable collage", e)
            null
        }
    }

    private fun writeBitmap(bitmap: Bitmap, out: OutputStream) {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }
}