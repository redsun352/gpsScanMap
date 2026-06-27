package com.arkeoscan.core.renderer.gl

import android.opengl.GLES20
import android.util.Log

object GlUtils {
    private const val TAG = "ArkeoScanGL"

    fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "Shader oluşturulamadı, type=$type")
            return 0
        }
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "Shader derleme hatası: $log")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0

        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader)
            return 0
        }

        val program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "Program oluşturulamadı")
            return 0
        }

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            Log.e(TAG, "Program link hatası: $log")
            GLES20.glDeleteProgram(program)
            return 0
        }

        // Shader nesneleri programa attach edildikten sonra ayrı ayrı silinebilir;
        // GPU sürücüsü program linklendikten sonra ihtiyaç duyduğu veriyi zaten kopyalar.
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        return program
    }

    fun checkGlError(op: String) {
        var error: Int
        while ((GLES20.glGetError().also { error = it }) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "$op: glError $error")
        }
    }
}
