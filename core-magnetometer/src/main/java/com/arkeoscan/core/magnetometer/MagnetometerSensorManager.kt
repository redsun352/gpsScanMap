package com.arkeoscan.core.magnetometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.arkeoscan.core.common.model.MagnetometerSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manyetometre sensör dinleyicisi. TYPE_MAGNETIC_FIELD kullanır (kalibre edilmiş,
 * geomanyetik alan + cihaz manyetik bozulmaları dahil ham µT değeri).
 *
 * NOT: TYPE_MAGNETIC_FIELD_UNCALIBRATED yerine kalibre edilmiş sensör tercih edilmiştir;
 * çünkü kalibre sensör Android'in hard-iron bias düzeltmesini otomatik uygular ve
 * saha kullanımında daha kararlı taban (baseline) üretir. İleride istenirse
 * UNCALIBRATED moduna da kolayca geçilebilir (bkz. useUncalibrated parametresi).
 */
@Singleton
class MagnetometerSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun hasMagnetometer(): Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null

    fun sampleFlow(useUncalibrated: Boolean = false): Flow<MagnetometerSample> = callbackFlow {
        val sensorType = if (useUncalibrated) {
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
        } else {
            Sensor.TYPE_MAGNETIC_FIELD
        }
        val sensor = sensorManager.getDefaultSensor(sensorType)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (sensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // UNCALIBRATED sensör 6 değer döner (x,y,z,bias_x,bias_y,bias_z);
                // sadece ilk üçü (ham ölçüm) kullanılır.
                trySend(
                    MagnetometerSample(
                        x = event.values[0],
                        y = event.values[1],
                        z = event.values[2],
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
