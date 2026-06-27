package com.arkeoscan.core.database.di

import android.content.Context
import androidx.room.Room
import com.arkeoscan.core.database.ArkeoScanDatabase
import com.arkeoscan.core.database.dao.AnomalyResultDao
import com.arkeoscan.core.database.dao.CameraSurveyPhotoDao
import com.arkeoscan.core.database.dao.MagnetometerCalibrationDao
import com.arkeoscan.core.database.dao.ScanPointDao
import com.arkeoscan.core.database.dao.ScanSessionDao
import com.arkeoscan.core.database.dao.SurfaceAnalysisResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArkeoScanDatabase {
        return Room.databaseBuilder(
            context,
            ArkeoScanDatabase::class.java,
            ArkeoScanDatabase.DATABASE_NAME
        )
            // Saha aracı: şema değişikliklerinde veri kaybı kabul edilebilir (henüz prod değil).
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideScanSessionDao(db: ArkeoScanDatabase): ScanSessionDao = db.scanSessionDao()

    @Provides
    @Singleton
    fun provideScanPointDao(db: ArkeoScanDatabase): ScanPointDao = db.scanPointDao()

    @Provides
    @Singleton
    fun provideAnomalyResultDao(db: ArkeoScanDatabase): AnomalyResultDao = db.anomalyResultDao()

    @Provides
    @Singleton
    fun provideCameraSurveyPhotoDao(db: ArkeoScanDatabase): CameraSurveyPhotoDao = db.cameraSurveyPhotoDao()

    @Provides
    @Singleton
    fun provideMagnetometerCalibrationDao(db: ArkeoScanDatabase): MagnetometerCalibrationDao =
        db.magnetometerCalibrationDao()

    @Provides
    @Singleton
    fun provideSurfaceAnalysisResultDao(db: ArkeoScanDatabase): SurfaceAnalysisResultDao =
        db.surfaceAnalysisResultDao()
}
