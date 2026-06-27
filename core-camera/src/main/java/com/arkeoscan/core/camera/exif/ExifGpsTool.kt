package com.arkeoscan.core.camera.exif

import androidx.exifinterface.media.ExifInterface
import com.arkeoscan.core.common.model.GeoPoint
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "GPS kaydet, EXIF oku" gereksinimi: çekilen fotoğrafa konum bilgisini EXIF
 * etiketleri olarak yazar ve daha sonra geri okuyabilir.
 */
@Singleton
class ExifGpsTool @Inject constructor() {

    fun writeGps(file: File, point: GeoPoint) {
        val exif = ExifInterface(file.absolutePath)
        exif.setGpsInfo(android.location.Location("arkeoscan").apply {
            latitude = point.latitude
            longitude = point.longitude
            point.altitude?.let { altitude = it }
            time = point.timestampMillis
        })
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, point.timestampMillis.toString())
        exif.saveAttributes()
    }

    fun readGps(file: File): GeoPoint? {
        val exif = ExifInterface(file.absolutePath)
        val latLong = FloatArray(2)
        val hasLatLong = exif.getLatLong(latLong)
        if (!hasLatLong) return null

        val altitude = exif.getAltitude(0.0)

        return GeoPoint(
            latitude = latLong[0].toDouble(),
            longitude = latLong[1].toDouble(),
            altitude = if (altitude != 0.0) altitude else null,
            timestampMillis = file.lastModified()
        )
    }
}
