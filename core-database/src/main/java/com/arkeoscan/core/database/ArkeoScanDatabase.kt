package com.arkeoscan.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arkeoscan.core.database.dao.AnomalyResultDao
import com.arkeoscan.core.database.dao.CameraSurveyPhotoDao
import com.arkeoscan.core.database.dao.MagnetometerCalibrationDao
import com.arkeoscan.core.database.dao.ScanPointDao
import com.arkeoscan.core.database.dao.ScanSessionDao
import com.arkeoscan.core.database.entity.AnomalyResultEntity
import com.arkeoscan.core.database.entity.CameraSurveyPhotoEntity
import com.arkeoscan.core.database.entity.MagnetometerCalibrationEntity
import com.arkeoscan.core.database.entity.ScanPointEntity
import com.arkeoscan.core.database.entity.ScanSessionEntity

@Database(
    entities = [
        ScanSessionEntity::class,
        ScanPointEntity::class,
        AnomalyResultEntity::class,
        CameraSurveyPhotoEntity::class,
        MagnetometerCalibrationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ArkeoScanDatabase : RoomDatabase() {
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun scanPointDao(): ScanPointDao
    abstract fun anomalyResultDao(): AnomalyResultDao
    abstract fun cameraSurveyPhotoDao(): CameraSurveyPhotoDao
    abstract fun magnetometerCalibrationDao(): MagnetometerCalibrationDao

    companion object {
        const val DATABASE_NAME = "arkeoscan.db"
    }
}
