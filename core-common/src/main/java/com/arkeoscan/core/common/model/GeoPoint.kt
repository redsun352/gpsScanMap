package com.arkeoscan.core.common.model

/**
 * Tek bir GPS örneği. Walk Scan sırasında her kayıt noktasında üretilir.
 *
 * @property latitude WGS84 enlem (derece)
 * @property longitude WGS84 boylam (derece)
 * @property altitude Deniz seviyesinden yükseklik (metre), null ise bilinmiyor
 * @property accuracyMeters Konum doğruluk yarıçapı (metre)
 * @property speedMps Anlık hız (m/s), yürüyüş tespiti ve outlier filtrelemede kullanılır
 * @property bearingDegrees Hareket yönü (derece, 0-360), null ise bilinmiyor
 * @property timestampMillis Epoch milisaniye
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracyMeters: Float = 0f,
    val speedMps: Float = 0f,
    val bearingDegrees: Float? = null,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    /**
     * Haversine formülü ile bu noktadan diğerine olan mesafeyi metre olarak hesaplar.
     */
    fun distanceMetersTo(other: GeoPoint): Double {
        val r = 6371000.0 // Dünya yarıçapı (metre)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1) * Math.cos(lat2) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    /**
     * Bu noktadan diğerine olan ilk yönü (bearing) derece olarak hesaplar (0-360, kuzey=0).
     */
    fun bearingTo(other: GeoPoint): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLon = Math.toRadians(other.longitude - longitude)

        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) -
            Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        val bearing = Math.toDegrees(Math.atan2(y, x))
        return (bearing + 360) % 360
    }
}
