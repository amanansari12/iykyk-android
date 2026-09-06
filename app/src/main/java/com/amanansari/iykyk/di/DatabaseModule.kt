package com.amanansari.iykyk.di

import android.content.Context
import androidx.room.Room
import com.amanansari.iykyk.data.local.AppDatabase
import com.amanansari.iykyk.data.local.SavedCollageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room can't be constructor-injected directly (it needs Room.databaseBuilder),
 * so it gets its own small Hilt module — everything else in the app uses
 * plain @Inject constructors.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "iykyk_database"
        ).build()
    }

    @Provides
    fun provideSavedCollageDao(database: AppDatabase): SavedCollageDao {
        return database.savedCollageDao()
    }
}
