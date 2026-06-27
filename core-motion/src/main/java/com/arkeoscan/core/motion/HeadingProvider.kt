package com.arkeoscan.core.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cihazın manyetik kuzeye göre baktığı yönü (heading, derece, 0-360) hesaplar.
 * TYPE_ROTATION_VECTOR sensörünü tercih eder (gyro+accel+mag sensör füzyonu,
 * donanım/Android tarafında zaten filtrelenmiştir ve TYPE_ORIENTATION'dan daha
 * kararlıdır); yoksa TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD çiftine düşer.
 */
@Singleton
class HeadingProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun headingFlow(): Flow<Float> = callbackFlow {
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationVectorSensor != null) {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthRadians = orientation[0]
                    val degrees = (Math.toDegrees(azimuthRadians.toDouble()).toFloat() + 360f) % 360f
                    trySend(degrees)
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }

            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
            awaitClose { sensorManager.unregisterListener(listener) }
        } else {
            // Fallback: accelerometer + magnetometer çifti
            val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            var hasGravity = false
            var hasGeomagnetic = false

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, gravity, 0, 3)
                            hasGravity = true
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                            hasGeomagnetic = true
                        }
                    }
                    if (hasGravity && hasGeomagnetic) {
                        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                        if (success) {
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            val degrees = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                            trySend(degrees)
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }

            accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            magSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

            awaitClose { sensorManager.unregisterListener(listener) }
        }
    }

    fun hasRotationVectorSensor(): Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
}
