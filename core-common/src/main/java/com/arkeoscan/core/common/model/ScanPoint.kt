package com.arkeoscan.core.common.model

/**
 * Walk Scan sırasında tek bir kayıt anında üretilen birleşik veri noktası.
 * GPS, manyetometre ve heading verisini bir araya getirir; analiz ve
 * grid/interpolasyon katmanının temel girdisidir.
 */
data class ScanPoint(
    val id: Long = 0,
    val sessionId: Long,
    val sequenceIndex: Int,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val gpsAccuracyMeters: Float,
    val magneticTotalMicroTesla: Float,
    val magneticX: Float,
    val magneticY: Float,
    val magneticZ: Float,
    val headingDegrees: Float?,
    val timestampMillis: Long
)

/**
 * Bir Walk Scan oturumunun meta verisi.
 */
data class ScanSession(
    val id: Long = 0,
    val name: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val polygon: Polygon? = null,
    val areaSquareMeters: Double? = null,
    val perimeterMeters: Double? = null,
    val gridResolutionMeters: Double = 1.0,
    val status: ScanSessionStatus = ScanSessionStatus.RUNNING
)

enum class ScanSessionStatus {
    RUNNING,
    PAUSED,
    STOPPED,
    ANALYZED
}
