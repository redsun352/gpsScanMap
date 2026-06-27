package com.arkeoscan.core.camera.surface

import android.graphics.Bitmap
import com.arkeoscan.core.common.model.SurfaceAnalysisResult
import com.arkeoscan.core.common.model.SurfaceNoteReason
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera Survey fotoğrafları için yüzey analizi motoru.
 *
 * "Yüzey analizine uygun modüler yapı" gereksinimi: ColorProfileAnalyzer,
 * VegetationIndexEstimator ve TextureRoughnessAnalyzer birbirinden bağımsız
 * modüllerdir; her biri kendi başına test edilebilir/değiştirilebilir
 * (örn. ileride gerçek bir ML tabanlı sınıflandırıcı bu üçünün yerine veya
 * yanına eklenebilir, arayüz/çağıran kod değişmeden).
 *
 * Bu motor üç sinyali birleştirip basit eşik tabanlı bir "dikkat çekici yüzey
 * yaması" bayrağı üretir. Bu eşikler kasıtlı olarak basit ve şeffaftır
 * (ML kara kutusu değil) — saha kullanıcısı sonucu sorguladığında "neden bu
 * fotoğraf işaretlendi" sorusuna açık bir cevap verilebilsin diye.
 *
 * ÖNEMLİ KISIT: Bu motor yeraltı yapısı TESPİT ETMEZ. Sadece fotoğraftaki
 * görünür yüzeyin, kendi içinde (bölgeler arası) renk/doku/bitki örtüsü
 * kontrastı taşıyıp taşımadığını ölçer. Sonuç "incelemeye değer yüzey
 * yaması" olarak raporlanır, kesin bir arkeolojik bulgu olarak DEĞİL.
 */
@Singleton
class SurfaceAnalysisEngine @Inject constructor(
    private val colorProfileAnalyzer: ColorProfileAnalyzer,
    private val vegetationIndexEstimator: VegetationIndexEstimator,
    private val textureRoughnessAnalyzer: TextureRoughnessAnalyzer
) {

    /**
     * @param vegetationContrastThreshold VARI ızgara-içi standart sapma eşiği.
     *   Bu değerin üzerinde, fotoğrafın farklı bölgeleri arasında belirgin
     *   bitki örtüsü farkı var demektir (örn. bir köşe yeşil, diğeri kurak).
     * @param colorBrightnessVarianceThreshold Parlaklık varyansı eşiği —
     *   yüksek varyans, fotoğrafta belirgin renk/ton blokları olduğunu gösterir.
     * @param textureRoughnessThreshold Doku gradyan eşiği — yüksek değer,
     *   yüzeyde düzensiz/taşlı bir desen olduğunu gösterir.
     */
    fun analyze(
        photoId: Long,
        bitmap: Bitmap,
        vegetationContrastThreshold: Float = 0.08f,
        colorBrightnessVarianceThreshold: Float = 900f,
        textureRoughnessThreshold: Float = 18f
    ): SurfaceAnalysisResult {
        val colorProfile = colorProfileAnalyzer.analyze(bitmap)
        val vegetationIndex = vegetationIndexEstimator.estimate(bitmap)
        val vegetationContrast = vegetationIndexEstimator.estimateGridVariance(bitmap)
        val textureRoughness = textureRoughnessAnalyzer.analyze(bitmap)

        val reasons = mutableListOf<SurfaceNoteReason>()

        if (vegetationContrast >= vegetationContrastThreshold) {
            reasons.add(SurfaceNoteReason.VEGETATION_CONTRAST)
        }
        if (colorProfile.brightnessVariance >= colorBrightnessVarianceThreshold) {
            reasons.add(SurfaceNoteReason.COLOR_CONTRAST)
        }
        if (textureRoughness >= textureRoughnessThreshold) {
            reasons.add(SurfaceNoteReason.TEXTURE_CONTRAST)
        }

        return SurfaceAnalysisResult(
            photoId = photoId,
            colorProfile = colorProfile,
            vegetationIndex = vegetationIndex,
            textureRoughness = textureRoughness,
            noteworthySurfacePatch = reasons.isNotEmpty(),
            noteReasons = reasons
        )
    }
}
