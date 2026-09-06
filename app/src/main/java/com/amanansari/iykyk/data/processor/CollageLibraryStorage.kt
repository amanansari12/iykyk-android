package com.amanansari.iykyk.data.processor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Persists a collage bitmap into the app's own internal storage so Room
 * can track it and it can be browsed later from the in-app Saved library.
 * Completely separate from MediaExporter, which writes the public,
 * user-facing gallery copy — deleting a saved-library entry never
 * touches the gallery photo, and vice versa.
 */
class CollageLibraryStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val libraryDir: File
        get() = File(context.filesDir, "saved_collages").apply {
            if (!exists()) mkdirs()
        }

    fun saveToLibrary(bitmap: Bitmap): String? {
        return try {
            val file = File(libraryDir, "collage_${System.currentTimeMillis()}.jpg")

            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            Log.d("CollageLibraryStorage", "Saved collage to library: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e("CollageLibraryStorage", "Failed saving collage to library", e)
            null
        }
    }

    fun deleteFromLibrary(filePath: String) {
        try {
            val deleted = File(filePath).delete()
            Log.d("CollageLibraryStorage", "Deleted $filePath: $deleted")
        } catch (e: Exception) {
            Log.e("CollageLibraryStorage", "Failed deleting collage file", e)
        }
    }

    fun getShareableUriFor(filePath: String): Uri? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("CollageLibraryStorage", "Failed preparing shareable uri", e)
            null
        }
    }
}
