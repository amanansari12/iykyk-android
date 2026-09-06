package com.amanansari.iykyk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A collage the user chose to keep in the app's own Saved library.
 * [filePath] points at a JPEG copy in internal app storage — separate
 * from whatever MediaExporter writes to the public gallery.
 */
@Entity(tableName = "saved_collages")
data class SavedCollageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val createdAt: Long,
    val identityCount: Int,
    val totalAppearances: Int,
    val videoDurationMs: Long
)
