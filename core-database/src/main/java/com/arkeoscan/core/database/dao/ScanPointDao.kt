package com.arkeoscan.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arkeoscan.core.database.entity.ScanPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: ScanPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<ScanPointEntity>)

    @Query("SELECT * FROM scan_points WHERE session_id = :sessionId ORDER BY sequence_index ASC")
    suspend fun getPointsForSession(sessionId: Long): List<ScanPointEntity>

    @Query("SELECT * FROM scan_points WHERE session_id = :sessionId ORDER BY sequence_index ASC")
    fun observePointsForSession(sessionId: Long): Flow<List<ScanPointEntity>>

    @Query("SELECT COUNT(*) FROM scan_points WHERE session_id = :sessionId")
    suspend fun countForSession(sessionId: Long): Int

    @Query("SELECT * FROM scan_points WHERE session_id = :sessionId ORDER BY sequence_index DESC LIMIT 1")
    suspend fun getLastPoint(sessionId: Long): ScanPointEntity?

    @Query("DELETE FROM scan_points WHERE session_id = :sessionId")
    suspend fun deleteAllForSession(sessionId: Long)
}
