package com.arkeoscan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "anomaly_results",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class AnomalyResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "center_latitude")
    val centerLatitude: Double,

    @ColumnInfo(name = "center_longitude")
    val centerLongitude: Double,

    @ColumnInfo(name = "deviation_sigma")
    val deviationSigma: Float,

    @ColumnInfo(name = "area_square_meters")
    val areaSquareMeters: Double,

    @ColumnInfo(name = "shape")
    val shape: String,

    @ColumnInfo(name = "continuity_score")
    val continuityScore: Float,

    @ColumnInfo(name = "neighborhood_score")
    val neighborhoodScore: Float,

    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float,

    @ColumnInfo(name = "label")
    val label: String = "Manyetik anomali"
)

@Entity(
    tableName = "camera_survey_photos",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class CameraSurveyPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double?,

    @ColumnInfo(name = "longitude")
    val longitude: Double?,

    @ColumnInfo(name = "captured_at_millis")
    val capturedAtMillis: Long
)

@Entity(tableName = "magnetometer_calibrations")
data class MagnetometerCalibrationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "mean_microtesla")
    val meanMicroTesla: Float,

    @ColumnInfo(name = "std_dev_microtesla")
    val stdDevMicroTesla: Float,

    @ColumnInfo(name = "min_microtesla")
    val minMicroTesla: Float,

    @ColumnInfo(name = "max_microtesla")
    val maxMicroTesla: Float,

    @ColumnInfo(name = "sample_count")
    val sampleCount: Int,

    @ColumnInfo(name = "calibrated_at_millis")
    val calibratedAtMillis: Long
)
