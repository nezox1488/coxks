package fun.rich.utils.display.render.systemrender.builders.states;

import java.awt.*;

public record QuadColorState(int color1, int color2, int color3, int color4) {

	public static final QuadColorState TRANSPARENT = new QuadColorState(0, 0, 0, 0);
	public static final QuadColorState WHITE = new QuadColorState(-1, -1, -1, -1);

	public QuadColorState(Color color1, Color color2, Color color3, Color color4) {
		this(color1.getRGB(), color2.getRGB(), color3.getRGB(), color4.getRGB());
	}

	public QuadColorState(Color color) {
		this(color, color, color, color);
	}

	public QuadColorState(int color) {
		this(color, color, color, color);
	}


	public static QuadColorState fromRgba(int r, int g, int b, int a) {
		int packedColor = (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
		return new QuadColorState(packedColor);
	}

	private static Color interpolate(Color c1, Color c2, float ratio) {
		ratio = Math.max(0, Math.min(1, ratio));
		int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
		int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
		int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
		int a = (int) (c1.getAlpha() * (1 - ratio) + c2.getAlpha() * ratio);
		return new Color(r, g, b, a);
	}


	public static QuadColorState vertical(Color topColor, Color bottomColor) {
		return new QuadColorState(topColor, bottomColor, bottomColor, topColor);
	}

	public static QuadColorState vertical(int topColor, int bottomColor) {
		return new QuadColorState(topColor, bottomColor, bottomColor, topColor);
	}


	public static QuadColorState horizontal(Color leftColor, Color rightColor) {
		return new QuadColorState(leftColor, leftColor, rightColor, rightColor);
	}

	public static QuadColorState horizontal(int leftColor, int rightColor) {
		return new QuadColorState(leftColor, leftColor, rightColor, rightColor);
	}


	public static QuadColorState animatedVertical(Color color1, Color color2, double durationSeconds) {
		double progress = (System.currentTimeMillis() % (durationSeconds * 1000.0)) / (durationSeconds * 1000.0);
		float blend = (float) (Math.sin(progress * 2.0 * Math.PI) * 0.5 + 0.5);

		Color topColor = interpolate(color1, color2, blend);
		Color bottomColor = interpolate(color2, color1, blend);

		return vertical(topColor, bottomColor);
	}


	public static QuadColorState animatedHorizontal(Color color1, Color color2, double durationSeconds) {
		double progress = (System.currentTimeMillis() % (durationSeconds * 1000.0)) / (durationSeconds * 1000.0);
		float blend = (float) (Math.sin(progress * 2.0 * Math.PI) * 0.5 + 0.5);

		Color leftColor = interpolate(color1, color2, blend);
		Color rightColor = interpolate(color2, color1, blend);

		return horizontal(leftColor, rightColor);
	}
}