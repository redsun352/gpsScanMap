package com.arkeoscan.core.renderer.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.arkeoscan.core.common.model.ColorScale
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.renderer.mesh.Surface3DMeshBuilder
import com.arkeoscan.core.renderer.shader.ShaderSource
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 3D Surface ekranı için GLSurfaceView.Renderer.
 * Marching-cubes benzeri (burada grid yüksekliği + üçgenleme ile) gerçek zamanlı
 * 3D yüzey gösterimi. Rotate/Zoom/Pan CameraController üzerinden; Shadow/Lighting
 * shader'daki directional-light hesaplamasıyla; Gradient ColorScaleMapper ile sağlanır.
 */
class Surface3DRenderer(
    private val cameraController: CameraController
) : GLSurfaceView.Renderer {

    private var program = 0
    private var positionHandle = 0
    private var normalHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0
    private var normalMatrixHandle = 0
    private var lightDirHandle = 0

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val normalMatrix3x3 = FloatArray(9)

    @Volatile
    private var meshData: Surface3DMeshBuilder.MeshData? = null

    @Volatile
    private var pendingGrid: InterpolatedGrid? = null
    private var colorScale: ColorScale = ColorScale.VIRIDIS
    private var heightScale: Float = 8.0f

    private var surfaceWidth = 1
    private var surfaceHeight = 1

    fun updateGrid(grid: InterpolatedGrid, colorScale: ColorScale, heightScale: Float = 8.0f) {
        this.pendingGrid = grid
        this.colorScale = colorScale
        this.heightScale = heightScale
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.06f, 0.06f, 0.08f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        program = GlUtils.createProgram(
            ShaderSource.SURFACE3D_VERTEX_SHADER,
            ShaderSource.SURFACE3D_FRAGMENT_SHADER
        )
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        normalMatrixHandle = GLES20.glGetUniformLocation(program, "uNormalMatrix")
        lightDirHandle = GLES20.glGetUniformLocation(program, "uLightDirection")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 1f, 1000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        pendingGrid?.let { grid ->
            meshData = Surface3DMeshBuilder.build(grid, colorScale, heightScale)
            pendingGrid = null
        }
        val mesh = meshData ?: run {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            return
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // Kamera: distance zoom ile, pitch/yaw rotation ile, pan ile
        val distance = 60f / cameraController.zoomFactor
        Matrix.setLookAtM(
            viewMatrix, 0,
            cameraController.panX, -distance, distance * 0.6f,
            cameraController.panX, cameraController.panY, 0f,
            0f, 0f, 1f
        )

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, cameraController.rotationXDegrees, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, cameraController.rotationZDegrees, 0f, 0f, 1f)

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        // Normal matrix (model matrisinin üst-sol 3x3'ü; uniform ölçekleme varsayımıyla
        // ters-transpoze yerine direkt kullanılabilir, performans için tercih edildi)
        extractNormalMatrix(modelMatrix, normalMatrix3x3)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix3fv(normalMatrixHandle, 1, false, normalMatrix3x3, 0)
        GLES20.glUniform3f(lightDirHandle, 0.4f, -0.6f, 1.0f)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.vertexBuffer)

        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.normalBuffer)

        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, mesh.colorBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun extractNormalMatrix(model: FloatArray, out: FloatArray) {
        // model 4x4 -> üst-sol 3x3 (column-major Android Matrix formatı)
        out[0] = model[0]; out[1] = model[1]; out[2] = model[2]
        out[3] = model[4]; out[4] = model[5]; out[5] = model[6]
        out[6] = model[8]; out[7] = model[9]; out[8] = model[10]
    }
}
