package com.dramix.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dramix.app.core.database.entity.DownloadRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDownload(record: DownloadRecordEntity): Long

    @Query("SELECT * FROM download_record WHERE media_id = :mediaId LIMIT 1")
    suspend fun getDownloadByMediaIdSync(mediaId: String): DownloadRecordEntity?

    @Query("SELECT * FROM download_record WHERE media_id = :mediaId LIMIT 1")
    fun getDownloadByMediaId(mediaId: String): Flow<DownloadRecordEntity?>

    @Query("SELECT * FROM download_record ORDER BY created_at DESC")
    fun getAllDownloads(): Flow<List<DownloadRecordEntity>>

    @Query("SELECT * FROM download_record WHERE status = :status ORDER BY created_at DESC")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadRecordEntity>>

    @Query("""
        UPDATE download_record 
        SET bytes_downloaded = :bytesDownloaded, 
            total_bytes = :totalBytes, 
            progress_percentage = :progressPercentage, 
            status = :status 
        WHERE media_id = :mediaId
    """)
    suspend fun updateDownloadProgress(
        mediaId: String,
        bytesDownloaded: Long,
        totalBytes: Long,
        progressPercentage: Int,
        status: String
    ): Int

    @Query("""
        UPDATE download_record 
        SET status = :status, 
            completed_at = :completedAt, 
            error_message = :errorMessage 
        WHERE media_id = :mediaId
    """)
    suspend fun updateDownloadStatus(
        mediaId: String,
        status: String,
        completedAt: Long? = null,
        errorMessage: String? = null
    ): Int

    @Query("DELETE FROM download_record WHERE media_id = :mediaId")
    suspend fun deleteDownload(mediaId: String): Int
}
