package engine.graphics.resources;

import engine.core.utils.FileUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.HashMap;

public final class Texture extends GLResource {

	private static final HashMap<String, Texture> TEXTURES = new HashMap<>();

	public static Texture getOrLoad(String file) {
		if(TEXTURES.containsKey(file)) {
			return TEXTURES.get(file);
		}
		Texture texture = loadTexture(file);
		TEXTURES.put(file, texture);
		return texture;
	}

	public final int id;

	public Texture(ByteBuffer pixels, int width, int height, TextureProperties properties) {
		this.id = GL15.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.id);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		properties.apply();
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
	}

	public Texture(ByteBuffer pixels, int width, int height) {
		this(pixels, width, height, new TextureProperties());
	}

	public Texture(float[] pixels, int width, int height, TextureProperties properties) {
		this.id = GL15.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.id);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		properties.apply();
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0, GL11.GL_RGB, GL11.GL_FLOAT, pixels);
	}

	public Texture(float[] pixels, int width, int height) {
		this(pixels, width, height, new TextureProperties());
	}

	public void bind() {
		this.bind(0);
	}

	public void bind(int textureUnit) {
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnit);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.id);
	}

	@Override
	public void delete() {
		GL11.glDeleteTextures(this.id);
	}

	private static Texture loadTexture(String file) {
		return FileUtils.readImage(file, image -> {
			ByteBuffer buffer = ByteBuffer.allocateDirect(4 * image.getWidth() * image.getHeight());
			for(int pixel : image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth())) {
				buffer.put((byte) ((pixel >> 16) & 0xff));
				buffer.put((byte) ((pixel >> 8) & 0xff));
				buffer.put((byte) (pixel & 0xff));
				buffer.put((byte) ((pixel >> 24) & 0xff));
			}
			return new Texture(buffer.flip(), image.getWidth(), image.getHeight(), new TextureProperties(file + ".properties"));
		}, exception -> {
			System.err.println("Error loading texture " + file);
			exception.printStackTrace();
			return new Texture(new float[] {0.0f,0.0f,0.0f, 1.0f,1.0f,1.0f, 1.0f,1.0f,1.0f, 0.0f,0.0f,0.0f}, 2, 2);
		});
	}
}