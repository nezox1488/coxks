package fun.rich.display.screens.clickgui.components.implement.themes;

import fun.rich.display.screens.clickgui.components.implement.themes.ThemeManager;

import java.awt.*;

public class ThemeColorsGetter {
    
    public static int getText() {
        return ThemeManager.getInstance().getText();
    }

    public static int getText(float alpha) {
        return ThemeManager.getInstance().getTextWithAlpha(alpha);
    }

    public static int getTextDim() {
        return ThemeManager.getInstance().getTextDim();
    }

    public static int getTextDim(float alpha) {
        return ThemeManager.getInstance().getTextDimWithAlpha(alpha);
    }

    public static int getPrimary() {
        return ThemeManager.getInstance().getPrimary();
    }

    public static int getSecondary() {
        return ThemeManager.getInstance().getSecondary();
    }

    public static int getAccent() {
        return ThemeManager.getInstance().getAccent();
    }

    public static int getBackground() {
        return ThemeManager.getInstance().getBackground();
    }

    public static int getGuiBackground() {
        return ThemeManager.getInstance().getGuiBackground();
    }

    public static int getLines() {
        return ThemeManager.getInstance().getLines();
    }

    public static int getHover() {
        return ThemeManager.getInstance().getHover();
    }

    public static int getGuiRectColor(float alpha) {
        int color = getGuiBackground();
        int a = (int) (alpha * 255);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static Color withAlpha(int color, int alpha) {
        return new Color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha);
    }

    public static int blend(int color1, int color2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        int a = (int) (a1 + (a2 - a1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}