package com.arkeoscan.core.reports.pdf

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Heatmap/Contour/3D Surface ekranlarından alınan Bitmap'i (GLSurfaceView
 * tarafında glReadPixels ile elde edilir, bkz. ui katmanındaki screenshot
 * yardımcı fonksiyonu) PNG dosyasına yazar.
 */
@Singleton
class PngExporter @Inject constructor() {

    fun export(bitmap: Bitmap, outputFile: File) {
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
