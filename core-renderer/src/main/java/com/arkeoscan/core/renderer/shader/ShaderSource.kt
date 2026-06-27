package com.arkeoscan.core.renderer.shader

/**
 * OpenGL ES 2.0 GLSL shader kaynakları.
 * - Heatmap: 2D düzlem üzerinde vertex-renk interpolasyonu (yumuşak renk geçişi GPU'da
 *   otomatik lineer interpolasyonla sağlanır, "renk geçişleri yumuşak olsun" gereksinimi).
 * - Surface3D: Gouraud shading (vertex-normal tabanlı) + tek yönlü directional light,
 *   "Shadow / Lighting / Gradient" gereksinimi için temel diffuse+ambient model.
 */
object ShaderSource {

    const val HEATMAP_VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        attribute vec4 aPosition;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vColor = aColor;
        }
    """

    const val HEATMAP_FRAGMENT_SHADER = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """

    const val SURFACE3D_VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        uniform mat3 uNormalMatrix;
        uniform vec3 uLightDirection;
        attribute vec4 aPosition;
        attribute vec3 aNormal;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * aPosition;

            vec3 normal = normalize(uNormalMatrix * aNormal);
            float diffuse = max(dot(normal, normalize(uLightDirection)), 0.0);
            float ambient = 0.35;
            float lighting = ambient + diffuse * 0.75;
            lighting = clamp(lighting, 0.0, 1.15);

            vColor = vec4(aColor.rgb * lighting, aColor.a);
        }
    """

    const val SURFACE3D_FRAGMENT_SHADER = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """

    // Contour çizgileri için sade tek renkli line shader
    const val LINE_VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        attribute vec4 aPosition;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
        }
    """

    const val LINE_FRAGMENT_SHADER = """
        precision mediump float;
        uniform vec4 uColor;
        void main() {
            gl_FragColor = uColor;
        }
    """
}
