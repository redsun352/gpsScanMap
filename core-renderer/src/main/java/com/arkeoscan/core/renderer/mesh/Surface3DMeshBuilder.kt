package com.arkeoscan.core.renderer.mesh

import com.arkeoscan.core.common.model.ColorScale
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.renderer.shader.ColorScaleMapper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * InterpolatedGrid'i 3D yüzey mesh'ine çevirir. Grid değeri Z eksenine (yükseklik)
 * map'lenir; her üçgen için yüzey normali hesaplanır (Gouraud/flat shading,
 * ShaderSource.SURFACE3D_VERTEX_SHADER ile birlikte kullanılır).
 *
 * "Rotate / Zoom / Pan / Shadow / Lighting / Gradient" gereksinimi: Rotate/Zoom/Pan
 * kamera matrisinde (GLRenderer/ModelMatrix) ele alınır; Shadow/Lighting bu mesh'in
 * normal verisi + shader'daki directional light ile sağlanır; Gradient ColorScaleMapper
 * ile renk olarak uygulanır.
 */
object Surface3DMeshBuilder {

    data class MeshData(
        val vertexBuffer: FloatBuffer,
        val normalBuffer: FloatBuffer,
        val colorBuffer: FloatBuffer,
        val vertexCount: Int
    )

    /**
     * @param heightScale Z eksenindeki görsel büyütme katsayısı (manyetik sapma değerleri
     *   genelde küçük (µT) olduğundan, görünür bir 3D yüzey için ölçeklenir).
     */
    fun build(grid: InterpolatedGrid, colorScale: ColorScale, heightScale: Float = 1.0f): MeshData {
        val range = (grid.maxValue - grid.minValue).let { if (it <= 0f) 1f else it }

        // Önce her hücre için 3D pozisyonu hesapla (NoData hücreler grid ortalamasına
        // düzleştirilir ki üçgenleşmede çökme/delik oluşmasın; renderda zaten polygon
        // dışı hücreler ayrı bir alfa/maskeyle gizlenebilir, burada basitlik için
        // mesh'e dahil edilir ama düz/yatay bırakılır).
        fun heightOf(value: Float, insidePolygon: Boolean): Float {
            if (!insidePolygon) return 0f
            val normalized = (value - grid.minValue) / range
            return normalized * heightScale
        }

        val positions = Array(grid.rows) { row ->
            Array(grid.cols) { col ->
                val cell = grid.cellAt(row, col)
                val x = cell?.localX?.toFloat() ?: 0f
                val y = cell?.localY?.toFloat() ?: 0f
                val z = heightOf(cell?.value ?: grid.meanValue, cell?.isInsidePolygon ?: false)
                floatArrayOf(x, y, z)
            }
        }

        val positionsList = ArrayList<Float>()
        val normalsList = ArrayList<Float>()
        val colorsList = ArrayList<Float>()

        fun addVertex(p: FloatArray, normal: FloatArray, color: ColorScaleMapper.Rgb) {
            positionsList.add(p[0]); positionsList.add(p[1]); positionsList.add(p[2])
            normalsList.add(normal[0]); normalsList.add(normal[1]); normalsList.add(normal[2])
            colorsList.add(color.r); colorsList.add(color.g); colorsList.add(color.b); colorsList.add(1f)
        }

        for (row in 0 until grid.rows - 1) {
            for (col in 0 until grid.cols - 1) {
                val c00 = grid.cellAt(row, col) ?: continue
                val c10 = grid.cellAt(row, col + 1) ?: continue
                val c01 = grid.cellAt(row + 1, col) ?: continue
                val c11 = grid.cellAt(row + 1, col + 1) ?: continue

                // Tamamen polygon dışındaysa bu quad'ı atla (delik bırak; "Polygon dışı görünmesin")
                if (!c00.isInsidePolygon && !c10.isInsidePolygon &&
                    !c01.isInsidePolygon && !c11.isInsidePolygon
                ) continue

                val p00 = positions[row][col]
                val p10 = positions[row][col + 1]
                val p01 = positions[row + 1][col]
                val p11 = positions[row + 1][col + 1]

                val color00 = ColorScaleMapper.map(colorScale, ((c00.value - grid.minValue) / range))
                val color10 = ColorScaleMapper.map(colorScale, ((c10.value - grid.minValue) / range))
                val color01 = ColorScaleMapper.map(colorScale, ((c01.value - grid.minValue) / range))
                val color11 = ColorScaleMapper.map(colorScale, ((c11.value - grid.minValue) / range))

                // Üçgen 1: p00, p10, p11
                val normal1 = computeNormal(p00, p10, p11)
                addVertex(p00, normal1, color00)
                addVertex(p10, normal1, color10)
                addVertex(p11, normal1, color11)

                // Üçgen 2: p00, p11, p01
                val normal2 = computeNormal(p00, p11, p01)
                addVertex(p00, normal2, color00)
                addVertex(p11, normal2, color11)
                addVertex(p01, normal2, color01)
            }
        }

        val vertexCount = positionsList.size / 3

        val vertexBuffer = toBuffer(positionsList)
        val normalBuffer = toBuffer(normalsList)
        val colorBuffer = toBuffer(colorsList)

        return MeshData(vertexBuffer, normalBuffer, colorBuffer, vertexCount)
    }

    private fun computeNormal(a: FloatArray, b: FloatArray, c: FloatArray): FloatArray {
        val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
        val vx = c[0] - a[0]; val vy = c[1] - a[1]; val vz = c[2] - a[2]

        // Cross product (u x v)
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx

        val length = Math.sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat()
        if (length > 1e-6f) {
            nx /= length; ny /= length; nz /= length
        } else {
            nx = 0f; ny = 0f; nz = 1f
        }
        return floatArrayOf(nx, ny, nz)
    }

    private fun toBuffer(values: List<Float>): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(values.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(values.toFloatArray())
        buffer.position(0)
        return buffer
    }
}
