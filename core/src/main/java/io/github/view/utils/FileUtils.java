package io.github.view.utils;

import io.github.view.resources.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileUtils {

	public static <T> T readFile(String file, Function<InputStream, T> action) {
		try(InputStream inputStream = Texture.class.getResourceAsStream(file)) {
			if(inputStream == null)
				throw new FileNotFoundException("Could not find file " + file);
			return action.apply(inputStream);
		} catch(IOException e) {
			throw new RuntimeException("Error loading file " + file, e);
		}
	}

	public static Stream<String> readLines(String file) {
		return readFile(file, inputStream -> {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				Stream<String> lines = reader.lines();
				reader.close();
				return lines;
			} catch (IOException e) {
				throw new RuntimeException("Error loading file " + file, e);
			}
		});
	}

	public static String readString(String file) {
		return readFile(file, inputStream -> {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				String lines = reader.lines().collect(Collectors.joining("\n"));
				reader.close();
				return lines;
			} catch (IOException e) {
				throw new RuntimeException("Error loading file " + file, e);
			}
		});
	}

	public static BufferedImage readImage(String file) {
		return readFile(file, inputStream -> {
			try {
				return ImageIO.read(inputStream);
			} catch (IOException e) {
				throw new RuntimeException("Error loading image " + file, e);
			}
		});
	}
}