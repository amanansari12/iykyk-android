package com.amanansari.iykyk.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCollageDao {

    @Insert
    suspend fun insert(collage: SavedCollageEntity): Long

    @Query("SELECT * FROM saved_collages ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedCollageEntity>>

    @Delete
    suspend fun delete(collage: SavedCollageEntity)
}
