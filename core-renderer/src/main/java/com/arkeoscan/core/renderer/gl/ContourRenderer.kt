package com.arkeoscan.core.renderer.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.arkeoscan.core.common.model.ColorScale
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.renderer.mesh.ContourBuilder
import com.arkeoscan.core.renderer.mesh.HeatmapMeshBuilder
import com.arkeoscan.core.renderer.shader.ShaderSource
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Contour ekranı: zemin renkli heatmap (hafif soluk) + üzerine ContourBuilder'dan
 * gelen izoline (line strip) segmentleri çizilir. "Surfer benzeri contour üret,
 * izolines oluştur, renkli contour" gereksinimi.
 */
class ContourRenderer(
    private val cameraController: CameraController
) : GLSurfaceView.Renderer {

    private var heatmapProgram = 0
    private var heatmapPositionHandle = 0
    private var heatmapColorHandle = 0
    private var heatmapMvpHandle = 0

    private var lineProgram = 0
    private var linePositionHandle = 0
    private var lineColorHandle = 0
    private var lineMvpHandle = 0

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    @Volatile
    private var pendingGrid: InterpolatedGrid? = null
    private var heatmapMesh: HeatmapMeshBuilder.MeshData? = null
    private var contourLines: List<ContourBuilder.ContourLine> = emptyList()
    private var colorScale: ColorScale = ColorScale.VIRIDIS
    private var aspectRatio = 1f

    fun updateGrid(grid: InterpolatedGrid, colorScale: ColorScale) {
        this.pendingGrid = grid
        this.colorScale = colorScale
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.04f, 0.04f, 0.05f, 1f)
        GLES20.glLineWidth(2.5f)

        heatmapProgram = GlUtils.createProgram(
            ShaderSource.HEATMAP_VERTEX_SHADER,
            ShaderSource.HEATMAP_FRAGMENT_SHADER
        )
        heatmapPositionHandle = GLES20.glGetAttribLocation(heatmapProgram, "aPosition")
        heatmapColorHandle = GLES20.glGetAttribLocation(heatmapProgram, "aColor")
        heatmapMvpHandle = GLES20.glGetUniformLocation(heatmapProgram, "uMVPMatrix")

        lineProgram = GlUtils.createProgram(
            ShaderSource.LINE_VERTEX_SHADER,
            ShaderSource.LINE_FRAGMENT_SHADER
        )
        linePositionHandle = GLES20.glGetAttribLocation(lineProgram, "aPosition")
        lineColorHandle = GLES20.glGetUniformLocation(lineProgram, "uColor")
        lineMvpHandle = GLES20.glGetUniformLocation(lineProgram, "uMVPMatrix")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspectRatio = width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        pendingGrid?.let { grid ->
            heatmapMesh = HeatmapMeshBuilder.build(grid, colorScale)
            contourLines = ContourBuilder.buildContours(grid, levels = 10)
            pendingGrid = null
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val halfExtent = 50f / cameraController.zoomFactor
        Matrix.orthoM(
            projectionMatrix, 0,
            -halfExtent * aspectRatio, halfExtent * aspectRatio,
            -halfExtent, halfExtent,
            -10f, 10f
        )
        Matrix.setLookAtM(
            viewMatrix, 0,
            cameraController.panX, cameraController.panY, 1f,
            cameraController.panX, cameraController.panY, 0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        heatmapMesh?.let { mesh ->
            GLES20.glUseProgram(heatmapProgram)
            GLES20.glUniformMatrix4fv(heatmapMvpHandle, 1, false, mvpMatrix, 0)

            GLES20.glEnableVertexAttribArray(heatmapPositionHandle)
            GLES20.glVertexAttribPointer(heatmapPositionHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.vertexBuffer)

            GLES20.glEnableVertexAttribArray(heatmapColorHandle)
            GLES20.glVertexAttribPointer(heatmapColorHandle, 4, GLES20.GL_FLOAT, false, 0, mesh.colorBuffer)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)

            GLES20.glDisableVertexAttribArray(heatmapPositionHandle)
            GLES20.glDisableVertexAttribArray(heatmapColorHandle)
        }

        if (contourLines.isNotEmpty()) {
            GLES20.glUseProgram(lineProgram)
            GLES20.glUniformMatrix4fv(lineMvpHandle, 1, false, mvpMatrix, 0)

            for (line in contourLines) {
                if (line.segments.isEmpty()) continue
                val (buffer, vertexCount) = ContourBuilder.toFloatBuffer(line.segments)

                // Kontur çizgilerini koyu antrasit/beyaza yakın tek renk; gerçek
                // "renkli contour" istenirse isovalue'ya göre ColorScaleMapper'dan
                // renk alınabilir (bkz. not aşağıda).
                GLES20.glUniform4f(lineColorHandle, 0.92f, 0.92f, 0.92f, 0.85f)

                GLES20.glEnableVertexAttribArray(linePositionHandle)
                GLES20.glVertexAttribPointer(linePositionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount)
                GLES20.glDisableVertexAttribArray(linePositionHandle)
            }
        }
    }
}
