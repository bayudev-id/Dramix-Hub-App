package com.dramix.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dramix.app.core.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmark WHERE drama_id = :dramaId AND provider_id = :providerId")
    suspend fun deleteBookmark(dramaId: String, providerId: String): Int

    @Query("SELECT * FROM bookmark ORDER BY created_at DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) > 0 FROM bookmark WHERE drama_id = :dramaId AND provider_id = :providerId")
    fun isBookmarked(dramaId: String, providerId: String): Flow<Boolean>

    @Query("SELECT * FROM bookmark WHERE drama_id = :dramaId AND provider_id = :providerId LIMIT 1")
    suspend fun getBookmark(dramaId: String, providerId: String): BookmarkEntity?
}
