package fun.rich.display.screens.clickgui.components.implement.themes;

import fun.rich.display.screens.clickgui.components.implement.themes.ThemeManager.ThemeColor;
import java.awt.*;

public class ThemeColorsGetter {

    private static ThemeManager getManager() {
        return ThemeManager.getInstance();
    }

    public static int getPrimary() {
        return getManager().getPrimary();
    }

    public static int getPrimary(float alpha) {
        return getManager().getPrimaryWithAlpha(alpha);
    }

    public static int getSecondary() {
        return getManager().getSecondary();
    }

    public static int getSecondary(float alpha) {
        return getManager().getSecondaryWithAlpha(alpha);
    }

    public static int getAccent() {
        return getManager().getAccent();
    }

    public static int getAccent(float alpha) {
        return getManager().getAccentWithAlpha(alpha);
    }

    public static int getText() {
        return getManager().getText();
    }

    public static int getText(float alpha) {
        return getManager().getTextWithAlpha(alpha);
    }

    public static int getTextDim() {
        return getManager().getTextDim();
    }

    public static int getTextDim(float alpha) {
        return getManager().getTextDimWithAlpha(alpha);
    }

    public static int getBackground() {
        return getManager().getBackground();
    }

    public static int getGuiBackground() {
        return getManager().getGuiBackground();
    }

    public static int getGuiBackground(float alpha) {
        return getManager().getGuiBackgroundWithAlpha(alpha);
    }

    public static int getLines() {
        return getManager().getLines();
    }

    public static int getGuiText() {
        return getManager().getGuiText();
    }

    public static int getHover() {
        return getManager().getHover();
    }

    public static int getEnabledModule() {
        return getManager().getEnabledModule();
    }

    public static int getEnabledModule(float alpha) {
        return getManager().getEnabledModuleWithAlpha(alpha);
    }

    public static int getDisabledModule() {
        return getManager().getDisabledModule();
    }

    public static int getDisabledModule(float alpha) {
        return getManager().getDisabledModuleWithAlpha(alpha);
    }

    public static int getColor(ThemeColor type) {
        return getManager().getColor(type);
    }

    public static int getColor(ThemeColor type, float alpha) {
        return getManager().getColorWithAlpha(type, alpha);
    }

    @Deprecated
    public static int getGuiRectColor(float alpha) {
        return getGuiBackground(alpha);
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

    public static int setAlpha(int color, float alpha) {
        int a = (int) (alpha * 255);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int darken(int color, float amount) {
        int r = Math.max(0, (int) (((color >> 16) & 0xFF) * (1 - amount)));
        int g = Math.max(0, (int) (((color >> 8) & 0xFF) * (1 - amount)));
        int b = Math.max(0, (int) ((color & 0xFF) * (1 - amount)));
        int a = (color >> 24) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lighten(int color, float amount) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * amount));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * amount));
        int b = Math.min(255, (int) ((color & 0xFF) + (255 - (color & 0xFF)) * amount));
        int a = (color >> 24) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}