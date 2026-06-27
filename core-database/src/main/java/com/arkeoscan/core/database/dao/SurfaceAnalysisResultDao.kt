package com.arkeoscan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arkeoscan.core.database.entity.SurfaceAnalysisResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurfaceAnalysisResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: SurfaceAnalysisResultEntity): Long

    @Query("SELECT * FROM surface_analysis_results WHERE photo_id = :photoId LIMIT 1")
    suspend fun getForPhoto(photoId: Long): SurfaceAnalysisResultEntity?

    @Query(
        """
        SELECT * FROM surface_analysis_results
        WHERE photo_id IN (SELECT id FROM camera_survey_photos WHERE session_id = :sessionId)
        ORDER BY analyzed_at_millis DESC
        """
    )
    fun observeForSession(sessionId: Long): Flow<List<SurfaceAnalysisResultEntity>>

    @Query("SELECT * FROM surface_analysis_results WHERE noteworthy_surface_patch = 1 ORDER BY analyzed_at_millis DESC")
    fun observeAllNoteworthy(): Flow<List<SurfaceAnalysisResultEntity>>
}
