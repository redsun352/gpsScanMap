package com.arkeoscan.core.renderer.gl

/**
 * Rotate / Zoom / Pan jestlerini tutan basit kamera durumu.
 * GLRenderer bu değerleri her frame'de Matrix.* fonksiyonlarıyla
 * view/projection matrisine uygular. UI katmanı (Compose AndroidView +
 * GestureDetector/ScaleGestureDetector) bu sınıfın update fonksiyonlarını çağırır.
 */
class CameraController {
    var rotationXDegrees: Float = 35f   // Yukarıdan bakış açısı (pitch)
        private set
    var rotationZDegrees: Float = 0f    // Yatay döndürme (yaw)
        private set
    var zoomFactor: Float = 1.0f
        private set
    var panX: Float = 0f
        private set
    var panY: Float = 0f
        private set

    fun rotate(deltaYawDegrees: Float, deltaPitchDegrees: Float) {
        rotationZDegrees = (rotationZDegrees + deltaYawDegrees) % 360f
        rotationXDegrees = (rotationXDegrees + deltaPitchDegrees).coerceIn(5f, 85f)
    }

    fun zoom(scaleFactor: Float) {
        zoomFactor = (zoomFactor * scaleFactor).coerceIn(0.2f, 8.0f)
    }

    fun pan(deltaX: Float, deltaY: Float) {
        panX += deltaX
        panY += deltaY
    }

    fun reset() {
        rotationXDegrees = 35f
        rotationZDegrees = 0f
        zoomFactor = 1.0f
        panX = 0f
        panY = 0f
    }
}
