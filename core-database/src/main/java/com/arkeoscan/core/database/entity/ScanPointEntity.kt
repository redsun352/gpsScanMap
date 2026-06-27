package com.arkeoscan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_points",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id"), Index("sequence_index")]
)
data class ScanPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "sequence_index")
    val sequenceIndex: Int,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "altitude")
    val altitude: Double? = null,

    @ColumnInfo(name = "gps_accuracy_meters")
    val gpsAccuracyMeters: Float,

    @ColumnInfo(name = "magnetic_total_microtesla")
    val magneticTotalMicroTesla: Float,

    @ColumnInfo(name = "magnetic_x")
    val magneticX: Float,

    @ColumnInfo(name = "magnetic_y")
    val magneticY: Float,

    @ColumnInfo(name = "magnetic_z")
    val magneticZ: Float,

    @ColumnInfo(name = "heading_degrees")
    val headingDegrees: Float? = null,

    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long
)
