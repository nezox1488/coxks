package fun.rich.utils.display.render.systemrender.builders.impl;

import fun.rich.utils.display.atlasfont.msdf.MsdfFont;
import fun.rich.utils.display.render.systemrender.renderers.impl.BuiltText;
import fun.rich.utils.display.render.systemrender.builders.AbstractBuilder;
import java.awt.*;

public final class TextBuilder extends AbstractBuilder<BuiltText> {

    private MsdfFont font;
    private String text;
    private float size;
    private float thickness;
    private int color;
    private float smoothness;
    private float spacing;
    private int outlineColor;
    private float outlineThickness;
    private boolean rainbow;
    private int rainbowColor1;
    private int rainbowColor2;

    public  TextBuilder font(MsdfFont font) {
        this.font = font;
        return this;
    }

    public TextBuilder text(String text) {
        this.text = text;
        return this;
    }

    public TextBuilder size(float size) {
        this.size = size;
        return this;
    }

    public TextBuilder thickness(float thickness) {
        this.thickness = thickness;
        return this;
    }

    public TextBuilder color(Color color) {
        return this.color(color.getRGB());
    }

    public TextBuilder color(int color) {
        this.color = color;
        return this;
    }

    public TextBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public TextBuilder spacing(float spacing) {
        this.spacing = spacing;
        return this;
    }

    public TextBuilder outline(Color color, float thickness) {
        return this.outline(color.getRGB(), thickness);
    }

    public TextBuilder outline(int color, float thickness) {
        this.outlineColor = color;
        this.outlineThickness = thickness;
        return this;
    }

    public TextBuilder rainbow(boolean rainbow) {
        this.rainbow = rainbow;
        return this;
    }

    public TextBuilder rainbowColors(int color1, int color2) {
        this.rainbowColor1 = color1;
        this.rainbowColor2 = color2;
        return this;
    }

    public TextBuilder rainbowColors(Color color1, Color color2) {
        return this.rainbowColors(color1.getRGB(), color2.getRGB());
    }

    @Override
    protected BuiltText _build() {
        return new BuiltText(
                this.font,
                this.text,
                this.size,
                this.thickness,
                this.color,
                this.smoothness,
                this.spacing,
                this.outlineColor,
                this.outlineThickness,
                this.rainbow,
                this.rainbowColor1,
                this.rainbowColor2
        );
    }

    @Override
    protected void reset() {
        this.font = null;
        this.text = "";
        this.size = 0.0f;
        this.thickness = 0.05f;
        this.color = -1;
        this.smoothness = 0.5f;
        this.spacing = 0.0f;
        this.outlineColor = 0;
        this.outlineThickness = 0.0f;
        this.rainbow = false;
        this.rainbowColor1 = 0;
        this.rainbowColor2 = 0;
    }

}