package com.arkeoscan.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir Walk Scan oturumunu temsil eder. Polygon, GeoJSON-benzeri serileştirilmiş
 * string olarak (lat/lon çiftleri "lat,lon;lat,lon;...") saklanır; basitlik ve
 * Room tip dönüştürücü yükünü azaltmak için tercih edilmiştir.
 */
@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "started_at_millis")
    val startedAtMillis: Long,

    @ColumnInfo(name = "ended_at_millis")
    val endedAtMillis: Long? = null,

    @ColumnInfo(name = "polygon_wkt")
    val polygonWkt: String? = null,

    @ColumnInfo(name = "area_square_meters")
    val areaSquareMeters: Double? = null,

    @ColumnInfo(name = "perimeter_meters")
    val perimeterMeters: Double? = null,

    @ColumnInfo(name = "grid_resolution_meters")
    val gridResolutionMeters: Double = 1.0,

    @ColumnInfo(name = "status")
    val status: String = "RUNNING"
)
