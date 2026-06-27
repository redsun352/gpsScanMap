package com.arkeoscan.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arkeoscan.core.database.entity.ScanSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ScanSessionEntity): Long

    @Update
    suspend fun update(session: ScanSessionEntity)

    @Delete
    suspend fun delete(session: ScanSessionEntity)

    @Query("SELECT * FROM scan_sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: Long): ScanSessionEntity?

    @Query("SELECT * FROM scan_sessions ORDER BY started_at_millis DESC")
    fun observeAll(): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM scan_sessions WHERE status = 'RUNNING' OR status = 'PAUSED' LIMIT 1")
    suspend fun getActiveSession(): ScanSessionEntity?
}
