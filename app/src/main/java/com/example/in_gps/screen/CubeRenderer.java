package com.example.in_gps.screen;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CubeRenderer implements GLSurfaceView.Renderer {

    // Euler angles from sensor (degrees)
    private volatile float rotX = 0f, rotY = 0f, rotZ = 0f;

    // Smoothed display angles
    private float smoothX = 0f, smoothY = 0f, smoothZ = 0f;
    private static final float SMOOTH = 0.15f;

    private int program;
    private int aPosition, aColor, uMVPMatrix;

    private FloatBuffer vertexBuf;
    private FloatBuffer colorBuf;
    private ShortBuffer indexBuf;

    private final float[] mvp  = new float[16];
    private final float[] proj = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp  = new float[16];

    // ── Shaders ────────────────────────────────────────────────────────────
    private static final String VS =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 aPosition;" +
        "attribute vec4 aColor;" +
        "varying vec4 vColor;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * aPosition;" +
        "  vColor = aColor;" +
        "}";

    private static final String FS =
        "precision mediump float;" +
        "varying vec4 vColor;" +
        "void main() { gl_FragColor = vColor; }";

    // ── Cube geometry (24 vertices, 4 per face) ────────────────────────────
    // Each face has its own 4 vertices for independent face coloring
    private static final float[] VERTICES = {
        // Front  (z=+0.5)
        -0.5f,-0.5f, 0.5f,   0.5f,-0.5f, 0.5f,   0.5f, 0.5f, 0.5f,  -0.5f, 0.5f, 0.5f,
        // Back   (z=-0.5)
         0.5f,-0.5f,-0.5f,  -0.5f,-0.5f,-0.5f,  -0.5f, 0.5f,-0.5f,   0.5f, 0.5f,-0.5f,
        // Left   (x=-0.5)
        -0.5f,-0.5f,-0.5f,  -0.5f,-0.5f, 0.5f,  -0.5f, 0.5f, 0.5f,  -0.5f, 0.5f,-0.5f,
        // Right  (x=+0.5)
         0.5f,-0.5f, 0.5f,   0.5f,-0.5f,-0.5f,   0.5f, 0.5f,-0.5f,   0.5f, 0.5f, 0.5f,
        // Top    (y=+0.5)
        -0.5f, 0.5f, 0.5f,   0.5f, 0.5f, 0.5f,   0.5f, 0.5f,-0.5f,  -0.5f, 0.5f,-0.5f,
        // Bottom (y=-0.5)
        -0.5f,-0.5f,-0.5f,   0.5f,-0.5f,-0.5f,   0.5f,-0.5f, 0.5f,  -0.5f,-0.5f, 0.5f,
    };

    // Face colors (r,g,b,a) × 4 vertices each
    private static final float[] FACE_COLORS = {
        // Front  — red
        1.0f, 0.48f, 0.44f, 1f,
        // Back   — blue
        0.47f, 0.75f, 1.0f, 1f,
        // Left   — purple
        0.82f, 0.66f, 1.0f, 1f,
        // Right  — green
        0.34f, 0.83f, 0.39f, 1f,
        // Top    — orange
        1.0f,  0.65f, 0.34f, 1f,
        // Bottom — white
        0.94f, 0.96f, 0.98f, 1f,
    };

    // 36 indices (2 triangles × 6 faces)
    private static final short[] INDICES = {
         0, 1, 2,  0, 2, 3,   // Front
         4, 5, 6,  4, 6, 7,   // Back
         8, 9,10,  8,10,11,   // Left
        12,13,14, 12,14,15,   // Right
        16,17,18, 16,18,19,   // Top
        20,21,22, 20,22,23,   // Bottom
    };

    // ── Public API ─────────────────────────────────────────────────────────
    public void setAngles(float x, float y, float z) {
        rotX = x; rotY = y; rotZ = z;
    }

    // ── GLSurfaceView.Renderer ─────────────────────────────────────────────
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.05f, 0.07f, 0.09f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Build vertex buffer
        vertexBuf = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuf.put(VERTICES).position(0);

        // Build color buffer (expand 1 color per face → 4 vertices per face)
        float[] colors = new float[24 * 4]; // 24 vertices × RGBA
        for (int face = 0; face < 6; face++) {
            float r = FACE_COLORS[face * 4];
            float g = FACE_COLORS[face * 4 + 1];
            float b = FACE_COLORS[face * 4 + 2];
            float a = FACE_COLORS[face * 4 + 3];
            for (int v = 0; v < 4; v++) {
                int base = (face * 4 + v) * 4;
                colors[base]     = r;
                colors[base + 1] = g;
                colors[base + 2] = b;
                colors[base + 3] = a;
            }
        }
        colorBuf = ByteBuffer.allocateDirect(colors.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorBuf.put(colors).position(0);

        // Build index buffer
        indexBuf = ByteBuffer.allocateDirect(INDICES.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        indexBuf.put(INDICES).position(0);

        // Compile shaders
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VS);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FS);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);

        aPosition  = GLES20.glGetAttribLocation(program, "aPosition");
        aColor     = GLES20.glGetAttribLocation(program, "aColor");
        uMVPMatrix = GLES20.glGetUniformLocation(program, "uMVPMatrix");

        Matrix.setLookAtM(view, 0, 0f, 1.2f, 3.5f, 0f, 0f, 0f, 0f, 1f, 0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.perspectiveM(proj, 0, 40f, ratio, 0.1f, 100f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Smooth interpolation
        smoothX += (rotX - smoothX) * SMOOTH;
        smoothY += (rotY - smoothY) * SMOOTH;
        smoothZ += (rotZ - smoothZ) * SMOOTH;

        // Model matrix: apply Euler ZYX rotation
        Matrix.setIdentityM(model, 0);
        Matrix.rotateM(model, 0, smoothZ, 0f, 0f, 1f);
        Matrix.rotateM(model, 0, smoothY, 0f, 1f, 0f);
        Matrix.rotateM(model, 0, smoothX, 1f, 0f, 0f);

        Matrix.multiplyMM(tmp, 0, view, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj, 0, tmp, 0);

        GLES20.glUseProgram(program);

        vertexBuf.position(0);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuf);

        colorBuf.position(0);
        GLES20.glEnableVertexAttribArray(aColor);
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, 0, colorBuf);

        GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, mvp, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, INDICES.length, GLES20.GL_UNSIGNED_SHORT, indexBuf);

        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aColor);
    }

    // ── Helper ─────────────────────────────────────────────────────────────
    private static int compileShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
