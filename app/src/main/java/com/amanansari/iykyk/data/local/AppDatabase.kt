package com.amanansari.iykyk.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedCollageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedCollageDao(): SavedCollageDao
}
