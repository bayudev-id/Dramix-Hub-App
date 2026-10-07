package com.dramix.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dramix.app.core.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWatchHistory(history: WatchHistoryEntity): Long

    @Query("SELECT * FROM watch_history WHERE drama_id = :dramaId AND provider_id = :providerId AND episode_number = :episodeNumber LIMIT 1")
    suspend fun getEpisodeHistory(dramaId: String, providerId: String, episodeNumber: Int): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE drama_id = :dramaId AND provider_id = :providerId ORDER BY episode_number ASC")
    fun getDramaHistory(dramaId: String, providerId: String): Flow<List<WatchHistoryEntity>>

    @Query("""
        SELECT * FROM watch_history 
        WHERE id IN (
            SELECT id FROM watch_history 
            GROUP BY drama_id, provider_id 
            HAVING updated_at = MAX(updated_at)
        ) 
        ORDER BY updated_at DESC 
        LIMIT :limit
    """)
    fun getLatestWatchedDramas(limit: Int = 20): Flow<List<WatchHistoryEntity>>

    @Query("DELETE FROM watch_history WHERE drama_id = :dramaId AND provider_id = :providerId")
    suspend fun deleteDramaHistory(dramaId: String, providerId: String): Int

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory(): Int
}
