package com.amanansari.iykyk.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.amanansari.iykyk.data.local.SavedCollageDao
import com.amanansari.iykyk.data.local.SavedCollageEntity
import com.amanansari.iykyk.data.processor.CollageLibraryStorage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SavedCollageRepository @Inject constructor(
    private val savedCollageDao: SavedCollageDao,
    private val collageLibraryStorage: CollageLibraryStorage
) {

    fun observeSavedCollages(): Flow<List<SavedCollageEntity>> {
        return savedCollageDao.observeAll()
    }

    /** Writes the bitmap into internal storage and records it in Room. Returns false on failure. */
    suspend fun saveCollage(
        bitmap: Bitmap,
        identityCount: Int,
        totalAppearances: Int,
        videoDurationMs: Long
    ): Boolean {
        val filePath = collageLibraryStorage.saveToLibrary(bitmap) ?: return false

        savedCollageDao.insert(
            SavedCollageEntity(
                filePath = filePath,
                createdAt = System.currentTimeMillis(),
                identityCount = identityCount,
                totalAppearances = totalAppearances,
                videoDurationMs = videoDurationMs
            )
        )

        return true
    }

    suspend fun deleteCollage(collage: SavedCollageEntity) {
        collageLibraryStorage.deleteFromLibrary(collage.filePath)
        savedCollageDao.delete(collage)
    }

    fun getShareableUri(collage: SavedCollageEntity): Uri? {
        return collageLibraryStorage.getShareableUriFor(collage.filePath)
    }
}
