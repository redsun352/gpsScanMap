package com.arkeoscan.core.renderer.mesh

import com.arkeoscan.core.common.model.ColorScale
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.renderer.shader.ColorScaleMapper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Bir InterpolatedGrid'i, OpenGL'e gönderilebilir düz (flat) vertex + renk buffer'lara
 * çevirir. Her grid hücresi 2 üçgen (6 vertex) olarak triangulate edilir.
 * Polygon dışı (NoData) hücreler mesh'e dahil edilmez — "Polygon dışı görünmesin" gereksinimi.
 */
object HeatmapMeshBuilder {

    data class MeshData(
        val vertexBuffer: FloatBuffer,
        val colorBuffer: FloatBuffer,
        val vertexCount: Int
    )

    fun build(grid: InterpolatedGrid, colorScale: ColorScale): MeshData {
        val positions = ArrayList<Float>()
        val colors = ArrayList<Float>()

        val range = (grid.maxValue - grid.minValue).let { if (it <= 0f) 1f else it }
        val half = grid.resolutionMeters.toFloat() / 2f

        for (row in 0 until grid.rows) {
            for (col in 0 until grid.cols) {
                val cell = grid.cellAt(row, col) ?: continue
                if (!cell.isInsidePolygon) continue

                val cx = cell.localX.toFloat()
                val cy = cell.localY.toFloat()
                val normalized = (cell.value - grid.minValue) / range
                val rgb = ColorScaleMapper.map(colorScale, normalized)

                // İki üçgen: (x-h,y-h)-(x+h,y-h)-(x+h,y+h) ve (x-h,y-h)-(x+h,y+h)-(x-h,y+h)
                val quadVertices = listOf(
                    cx - half to cy - half,
                    cx + half to cy - half,
                    cx + half to cy + half,
                    cx - half to cy - half,
                    cx + half to cy + half,
                    cx - half to cy + half
                )

                for ((vx, vy) in quadVertices) {
                    positions.add(vx)
                    positions.add(vy)
                    positions.add(0f) // Z=0, heatmap düz düzlemde

                    colors.add(rgb.r)
                    colors.add(rgb.g)
                    colors.add(rgb.b)
                    colors.add(1f) // alpha
                }
            }
        }

        val vertexCount = positions.size / 3

        val vertexBuffer = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(positions.toFloatArray())
        vertexBuffer.position(0)

        val colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        colorBuffer.put(colors.toFloatArray())
        colorBuffer.position(0)

        return MeshData(vertexBuffer, colorBuffer, vertexCount)
    }
}
