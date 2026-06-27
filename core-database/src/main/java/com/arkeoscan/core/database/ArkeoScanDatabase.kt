package com.arkeoscan.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arkeoscan.core.database.dao.AnomalyResultDao
import com.arkeoscan.core.database.dao.CameraSurveyPhotoDao
import com.arkeoscan.core.database.dao.MagnetometerCalibrationDao
import com.arkeoscan.core.database.dao.ScanPointDao
import com.arkeoscan.core.database.dao.ScanSessionDao
import com.arkeoscan.core.database.dao.SurfaceAnalysisResultDao
import com.arkeoscan.core.database.entity.AnomalyResultEntity
import com.arkeoscan.core.database.entity.CameraSurveyPhotoEntity
import com.arkeoscan.core.database.entity.MagnetometerCalibrationEntity
import com.arkeoscan.core.database.entity.ScanPointEntity
import com.arkeoscan.core.database.entity.ScanSessionEntity
import com.arkeoscan.core.database.entity.SurfaceAnalysisResultEntity

@Database(
    entities = [
        ScanSessionEntity::class,
        ScanPointEntity::class,
        AnomalyResultEntity::class,
        CameraSurveyPhotoEntity::class,
        MagnetometerCalibrationEntity::class,
        SurfaceAnalysisResultEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class ArkeoScanDatabase : RoomDatabase() {
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun scanPointDao(): ScanPointDao
    abstract fun anomalyResultDao(): AnomalyResultDao
    abstract fun cameraSurveyPhotoDao(): CameraSurveyPhotoDao
    abstract fun magnetometerCalibrationDao(): MagnetometerCalibrationDao
    abstract fun surfaceAnalysisResultDao(): SurfaceAnalysisResultDao

    companion object {
        const val DATABASE_NAME = "arkeoscan.db"
    }
}
