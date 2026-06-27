package com.arkeoscan.core.renderer.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.arkeoscan.core.common.model.ColorScale
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.renderer.mesh.HeatmapMeshBuilder
import com.arkeoscan.core.renderer.shader.ShaderSource
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Heatmap ekranı için GLSurfaceView.Renderer. Ortografik (2D, üstten görünüm)
 * projeksiyon kullanır; Pan/Zoom CameraController'ın panX/panY/zoomFactor alanlarıyla
 * paylaşılır (rotationX/Z heatmap'te kullanılmaz, sabit üstten bakış).
 */
class HeatmapRenderer(
    private val cameraController: CameraController
) : GLSurfaceView.Renderer {

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    @Volatile
    private var pendingGrid: InterpolatedGrid? = null
    private var meshData: HeatmapMeshBuilder.MeshData? = null
    private var colorScale: ColorScale = ColorScale.VIRIDIS

    private var aspectRatio = 1f

    fun updateGrid(grid: InterpolatedGrid, colorScale: ColorScale) {
        this.pendingGrid = grid
        this.colorScale = colorScale
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.04f, 0.04f, 0.05f, 1f)
        program = GlUtils.createProgram(
            ShaderSource.HEATMAP_VERTEX_SHADER,
            ShaderSource.HEATMAP_FRAGMENT_SHADER
        )
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspectRatio = width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        pendingGrid?.let { grid ->
            meshData = HeatmapMeshBuilder.build(grid, colorScale)
            pendingGrid = null
        }
        val mesh = meshData ?: run {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

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

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.vertexBuffer)

        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, mesh.colorBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
}
