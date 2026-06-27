package com.arkeoscan.core.common.model

/**
 * Bir Camera Survey fotoğrafının yüzeysel analiz sonucu.
 *
 * ÖNEMLİ KISIT: Bu analiz SADECE fotoğrafın görünür yüzeyindeki renk, doku ve
 * bitki örtüsü farklılıklarına dayanır. Yeraltında herhangi bir yapı (oda,
 * duvar, tünel vb.) olduğuna dair hiçbir çıkarım yapmaz ve yapamaz — kamera
 * görünür ışıkla çalışır, toprağın altını göremez. Bu sınıfın ürettiği
 * "noteworthySurfacePatch" bayrağı yalnızca "bu fotoğraftaki yüzey, çevresinden
 * görsel olarak farklı görünüyor, sahada gözle incelenmeye değer olabilir"
 * anlamına gelir.
 */
data class SurfaceAnalysisResult(
    val photoId: Long = 0,
    val colorProfile: ColorProfile,
    val vegetationIndex: Float,
    val textureRoughness: Float,
    val noteworthySurfacePatch: Boolean,
    val noteReasons: List<SurfaceNoteReason>,
    val analyzedAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val DISCLAIMER =
            "Bu sonuç yalnızca fotoğraftaki yüzeysel renk/doku/bitki örtüsü " +
                "farklılığını gösterir; yeraltında oda, duvar, tünel veya benzeri " +
                "bir yapının varlığına dair hiçbir kanıt sunmaz. Kamera görünür " +
                "ışıkla çalışır ve toprağın altını göremez."
    }
}

/**
 * Fotoğrafın renk istatistikleri (sRGB ortalama + parlaklık varyansı).
 */
data class ColorProfile(
    val meanRed: Float,
    val meanGreen: Float,
    val meanBlue: Float,
    val brightnessVariance: Float
)

/**
 * Bir yüzey yamasının "dikkat çekici" işaretlenme nedenleri. Birden fazla
 * neden aynı anda geçerli olabilir (örn. hem bitki örtüsü farkı hem doku farkı).
 */
enum class SurfaceNoteReason {
    VEGETATION_CONTRAST,   // Çevresine göre belirgin bitki örtüsü/kuraklık farkı
    COLOR_CONTRAST,        // Çevresine göre belirgin renk/ton farkı (toprak rengi değişimi)
    TEXTURE_CONTRAST        // Çevresine göre belirgin doku/pürüzlülük farkı (taş dizilimi vb.)
}
