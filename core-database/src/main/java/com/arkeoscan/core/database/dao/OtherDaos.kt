package com.arkeoscan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arkeoscan.core.database.entity.AnomalyResultEntity
import com.arkeoscan.core.database.entity.CameraSurveyPhotoEntity
import com.arkeoscan.core.database.entity.MagnetometerCalibrationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnomalyResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<AnomalyResultEntity>)

    @Query("SELECT * FROM anomaly_results WHERE session_id = :sessionId ORDER BY confidence_score DESC")
    fun observeForSession(sessionId: Long): Flow<List<AnomalyResultEntity>>

    @Query("DELETE FROM anomaly_results WHERE session_id = :sessionId")
    suspend fun deleteAllForSession(sessionId: Long)
}

@Dao
interface CameraSurveyPhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: CameraSurveyPhotoEntity): Long

    @Query("SELECT * FROM camera_survey_photos WHERE session_id = :sessionId ORDER BY captured_at_millis ASC")
    fun observeForSession(sessionId: Long): Flow<List<CameraSurveyPhotoEntity>>
}

@Dao
interface MagnetometerCalibrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(calibration: MagnetometerCalibrationEntity): Long

    @Query("SELECT * FROM magnetometer_calibrations ORDER BY calibrated_at_millis DESC LIMIT 1")
    suspend fun getLatest(): MagnetometerCalibrationEntity?
}
