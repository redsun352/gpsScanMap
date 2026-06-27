package com.arkeoscan.core.gps.tracker

import android.annotation.SuppressLint
import android.content.Context
import com.arkeoscan.core.common.model.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Walk Scan sırasında canlı GPS akışı sağlar. Her "her adımda veya her 1 metrede"
 * kayıt prensibi burada minIntervalMillis ve minDisplacementMeters ile uygulanır;
 * gerçek mesafe filtrelemesi OutlierFilter ile birlikte çalışır (bkz. PolygonBuilder).
 */
@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun trackLocation(
        minIntervalMillis: Long = 1000L,
        minDisplacementMeters: Float = 1.0f
    ): Flow<GeoPoint> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, minIntervalMillis)
            .setMinUpdateDistanceMeters(minDisplacementMeters)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(
                        GeoPoint(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            altitude = if (loc.hasAltitude()) loc.altitude else null,
                            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0f,
                            speedMps = if (loc.hasSpeed()) loc.speed else 0f,
                            bearingDegrees = if (loc.hasBearing()) loc.bearing else null,
                            timestampMillis = loc.time
                        )
                    )
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, context.mainLooper)

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Tek seferlik anlık konum (örn. Camera Survey EXIF etiketleme için).
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): GeoPoint? {
        return try {
            val location = fusedClient.lastLocation.await()
            location?.let {
                GeoPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitude = if (it.hasAltitude()) it.altitude else null,
                    accuracyMeters = if (it.hasAccuracy()) it.accuracy else 0f,
                    timestampMillis = it.time
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
