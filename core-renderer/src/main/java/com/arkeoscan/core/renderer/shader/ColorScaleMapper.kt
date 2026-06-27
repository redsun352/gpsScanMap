package com.arkeoscan.core.renderer.shader

import com.arkeoscan.core.common.model.ColorScale

/**
 * Normalize edilmiş [0,1] değeri RGB (0..1 float) rengine çevirir.
 * "Profesyonel renk skalası" ve "renk geçişleri yumuşak olsun" gereksinimi
 * için çok-noktalı (multi-stop) lineer interpolasyon kullanılır.
 */
object ColorScaleMapper {

    data class Rgb(val r: Float, val g: Float, val b: Float)

    private val viridisStops = listOf(
        0.00f to Rgb(0.267f, 0.005f, 0.329f),
        0.25f to Rgb(0.282f, 0.140f, 0.458f),
        0.50f to Rgb(0.165f, 0.471f, 0.558f),
        0.75f to Rgb(0.478f, 0.821f, 0.318f),
        1.00f to Rgb(0.993f, 0.906f, 0.144f)
    )

    private val thermalStops = listOf(
        0.00f to Rgb(0.0f, 0.0f, 0.3f),
        0.25f to Rgb(0.0f, 0.4f, 0.9f),
        0.50f to Rgb(0.9f, 0.9f, 0.0f),
        0.75f to Rgb(1.0f, 0.5f, 0.0f),
        1.00f to Rgb(0.8f, 0.0f, 0.0f)
    )

    private val grayscaleStops = listOf(
        0.00f to Rgb(0.05f, 0.05f, 0.05f),
        1.00f to Rgb(0.95f, 0.95f, 0.95f)
    )

    // Golden Software Surfer "rainbow classic" tarzı: mavi -> yeşil -> sarı -> kırmızı
    private val surferClassicStops = listOf(
        0.00f to Rgb(0.10f, 0.10f, 0.60f),
        0.20f to Rgb(0.0f, 0.55f, 0.85f),
        0.40f to Rgb(0.0f, 0.75f, 0.40f),
        0.60f to Rgb(0.95f, 0.95f, 0.15f),
        0.80f to Rgb(0.95f, 0.55f, 0.10f),
        1.00f to Rgb(0.80f, 0.05f, 0.05f)
    )

    fun map(scale: ColorScale, normalizedValue: Float): Rgb {
        val v = normalizedValue.coerceIn(0f, 1f)
        val stops = when (scale) {
            ColorScale.VIRIDIS -> viridisStops
            ColorScale.THERMAL -> thermalStops
            ColorScale.GRAYSCALE -> grayscaleStops
            ColorScale.SURFER_CLASSIC -> surferClassicStops
        }
        return interpolateStops(stops, v)
    }

    private fun interpolateStops(stops: List<Pair<Float, Rgb>>, v: Float): Rgb {
        for (i in 0 until stops.size - 1) {
            val (pos1, color1) = stops[i]
            val (pos2, color2) = stops[i + 1]
            if (v in pos1..pos2) {
                val t = if (pos2 - pos1 > 0f) (v - pos1) / (pos2 - pos1) else 0f
                return Rgb(
                    r = lerp(color1.r, color2.r, t),
                    g = lerp(color1.g, color2.g, t),
                    b = lerp(color1.b, color2.b, t)
                )
            }
        }
        return stops.last().second
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
