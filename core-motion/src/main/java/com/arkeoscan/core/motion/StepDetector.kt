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
 * "Her adımda... kayıt oluştursun" gereksinimi için TYPE_STEP_DETECTOR sensörünü
 * sarmalar. Her tetiklenmede bir Unit emit eder (bir adım algılandı sinyali).
 * Cihazda donanım step detector yoksa (hasStepDetector() == false), WalkScan
 * ViewModel zaman/mesafe bazlı tetikleyiciye (her 1 metre / her 1 saniye) düşmelidir.
 */
@Singleton
class StepDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun hasStepDetector(): Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

    fun stepEventFlow(): Flow<Unit> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (sensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(Unit)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
