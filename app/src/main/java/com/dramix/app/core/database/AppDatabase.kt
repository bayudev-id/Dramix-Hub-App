package com.dramix.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dramix.app.core.database.dao.BookmarkDao
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.DownloadRecordEntity
import com.dramix.app.core.database.entity.WatchHistoryEntity

@Database(
    entities = [
        WatchHistoryEntity::class,
        BookmarkEntity::class,
        DownloadRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadRecordDao(): DownloadRecordDao

    companion object {
        private const val DATABASE_NAME = "dramix_local.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
