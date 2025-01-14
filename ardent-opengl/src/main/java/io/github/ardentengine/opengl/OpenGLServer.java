package io.github.ardentengine.opengl;

import io.github.ardentengine.core.math.Matrix2x3;
import io.github.ardentengine.core.math.Matrix3x4;
import io.github.ardentengine.core.math.Vector2;
import io.github.ardentengine.core.rendering.Material;
import io.github.ardentengine.core.rendering.Mesh;
import io.github.ardentengine.core.rendering.RenderingServer;
import io.github.ardentengine.core.rendering.Texture;
import io.github.ardentengine.core.scene.Camera2D;
import io.github.ardentengine.core.scene.Camera3D;
import io.github.ardentengine.core.scene.PointLight3D;
import org.lwjgl.opengl.GL11;

public class OpenGLServer extends RenderingServer {

    @Override
    public void setDefaultClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void setCamera(Camera3D camera) {
        // TODO: Store an instance of the camera and set the uniform buffer before rendering to account for shaders loaded after the camera has been set
        var viewMatrix = camera.viewMatrix();
        var projectionMatrix = camera.projectionMatrix();
        // TODO: The camera position needs to be set in the shader for specular lighting
        ShaderProgram.setBuffer("Camera3D", new float[] {
            viewMatrix.m00(), viewMatrix.m10(), viewMatrix.m20(), viewMatrix.m30(),
            viewMatrix.m01(), viewMatrix.m11(), viewMatrix.m21(), viewMatrix.m31(),
            viewMatrix.m02(), viewMatrix.m12(), viewMatrix.m22(), viewMatrix.m32(),
            viewMatrix.m03(), viewMatrix.m13(), viewMatrix.m23(), viewMatrix.m33(),
            projectionMatrix.m00(), projectionMatrix.m10(), projectionMatrix.m20(), projectionMatrix.m30(),
            projectionMatrix.m01(), projectionMatrix.m11(), projectionMatrix.m21(), projectionMatrix.m31(),
            projectionMatrix.m02(), projectionMatrix.m12(), projectionMatrix.m22(), projectionMatrix.m32(),
            projectionMatrix.m03(), projectionMatrix.m13(), projectionMatrix.m23(), projectionMatrix.m33()
        });
    }

    @Override
    public void draw(Mesh mesh, Material materialOverride, Matrix3x4 transform) {
        Renderer3D.getInstance().addToBatch(mesh, materialOverride, transform);
    }

    @Override
    public void setCamera(Camera2D camera) {
        // TODO: Store an instance of the camera and set the uniform buffer before rendering to account for shaders loaded after the camera has been set
        var viewMatrix = camera.viewMatrix();
        var projectionMatrix = camera.projectionMatrix();
        ShaderProgram.setBuffer("Camera2D", new float[] {
            viewMatrix.m00(), viewMatrix.m10(), viewMatrix.m20(), 0.0f,
            viewMatrix.m01(), viewMatrix.m11(), viewMatrix.m21(), 0.0f,
            viewMatrix.m02(), viewMatrix.m12(), viewMatrix.m22(), 0.0f,
            projectionMatrix.m00(), projectionMatrix.m10(), projectionMatrix.m20(), projectionMatrix.m30(),
            projectionMatrix.m01(), projectionMatrix.m11(), projectionMatrix.m21(), projectionMatrix.m31(),
            projectionMatrix.m02(), projectionMatrix.m12(), projectionMatrix.m22(), projectionMatrix.m32(),
            projectionMatrix.m03(), projectionMatrix.m13(), projectionMatrix.m23(), projectionMatrix.m33()
        });
        // TODO: The 2D camera should have a default value
    }

    @Override
    public void draw(Texture texture, Material material, Vector2 vertexOffset, Vector2 vertexScale, Vector2 uvOffset, Vector2 uvScale, Matrix2x3 transform) {
        Renderer2D.getInstance().addToBatch(new DrawData2D(texture, vertexOffset, vertexScale, uvOffset, uvScale, transform), material);
    }

    @Override
    public void update(Mesh mesh) {
        MeshData.requestUpdate(mesh);
    }

    @Override
    public void update(Texture texture) {
        TextureData.requestUpdate(texture);
    }

    @Override
    public void updateLight(PointLight3D light) {
        Renderer3D.getInstance().addToBatch(light);
    }
}