package com.arkeoscan.core.common.model

enum class DistanceUnit { METERS, FEET }

enum class ColorScale {
    VIRIDIS,
    THERMAL,
    GRAYSCALE,
    SURFER_CLASSIC
}

enum class GridResolution(val meters: Double, val label: String) {
    HALF_METER(0.5, "0.5 m"),
    ONE_METER(1.0, "1 m"),
    TWO_METER(2.0, "2 m")
}

enum class GpsAccuracyMode(val label: String, val minAccuracyMeters: Float) {
    HIGH("Yüksek (GPS+GLONASS)", 3f),
    BALANCED("Dengeli", 8f),
    LOW_POWER("Düşük Güç", 15f)
}

data class AppSettings(
    val gpsAccuracyMode: GpsAccuracyMode = GpsAccuracyMode.HIGH,
    val gridResolution: GridResolution = GridResolution.ONE_METER,
    val colorScale: ColorScale = ColorScale.VIRIDIS,
    val distanceUnit: DistanceUnit = DistanceUnit.METERS,
    val darkTheme: Boolean = true
)
