package fun.rich.display.screens.clickgui.components.implement.themes;

import fun.rich.display.screens.clickgui.components.implement.window.AbstractWindow;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.common.animation.Animation;
import fun.rich.common.animation.implement.Decelerate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import java.awt.Color;
import static fun.rich.common.animation.Direction.BACKWARDS;
import static fun.rich.common.animation.Direction.FORWARDS;
import static fun.rich.utils.display.font.Fonts.Type.GUIICONS;

public class PreviewWindow extends AbstractWindow {
    private boolean moduleEnabled = true;
    private final Animation colorAnimation = new Decelerate().setMs(400).setValue(9);
    private final Animation alphaAnimation = new Decelerate().setMs(400).setValue(105);
    private final Animation statusAlphaAnimation = new Decelerate().setMs(400).setValue(100);
    private final Animation statusStencilAnimation = new Decelerate().setMs(200).setValue(8);
    private final Animation statusSliderAnimation = new Decelerate().setMs(225).setValue(8);

    public PreviewWindow() {
        draggable(true);
        colorAnimation.setDirection(FORWARDS);
        alphaAnimation.setDirection(FORWARDS);
        statusAlphaAnimation.setDirection(FORWARDS);
        statusStencilAnimation.setDirection(FORWARDS);
        statusSliderAnimation.setDirection(FORWARDS);
        colorAnimation.reset();
        alphaAnimation.reset();
        statusAlphaAnimation.reset();
        statusStencilAnimation.reset();
        statusSliderAnimation.reset();
    }

    @Override
    public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        blur.render(ShapeProperties.create(matrix, x, y, width, 233).round(8).quality(8)
                .color(ThemeColorsGetter.getGuiBackground())
                .build());

        rectangle.render(ShapeProperties.create(matrix, x, y, width, 233).round(8)
                .color(ThemeColorsGetter.getGuiBackground())
                .build());


        rectangle.render(ShapeProperties.create(matrix, x, y + 22, width, 0.5f)
                .color(ThemeColorsGetter.getLines())
                .build());

        Fonts.getSize(15, Fonts.Type.SEMI).drawGradientString(matrix, "Theme Preview",
                x + 11, y + 10, ThemeColorsGetter.getText(), new Color(255, 255, 255, 255).getRGB());

        Fonts.getSize(17, Fonts.Type.ICONS).drawString(matrix, "K", x + width - 20, y + 10, ColorAssist.getText(1f));

        renderExampleModule(context, matrix, mouseX, mouseY);
        renderExampleSettings(context, matrix);
    }

    private void renderExampleModule(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        float moduleX = x + 10;
        float moduleY = y + 35;
        float moduleWidth = width - 20;
        float moduleHeight = 48;

        colorAnimation.setDirection(moduleEnabled ? FORWARDS : BACKWARDS);
        alphaAnimation.setDirection(moduleEnabled ? FORWARDS : BACKWARDS);
        int brightnessOffset = colorAnimation.getOutput().intValue();

        int enabledColor = ThemeColorsGetter.getEnabledModule();
        int disabledColor = ThemeColorsGetter.getDisabledModule();

        int topLeftColor = disabledColor;
        int topRightColor = moduleEnabled ?
                new Color(
                        Math.min(((enabledColor >> 16) & 0xFF) + brightnessOffset, 255),
                        Math.min(((enabledColor >> 8) & 0xFF) + brightnessOffset, 255),
                        Math.min((enabledColor & 0xFF) + brightnessOffset, 255),
                        (enabledColor >> 24) & 0xFF
                ).getRGB() : disabledColor;
        int bottomLeftColor = disabledColor;
        int bottomRightColor = topRightColor;

        rectangle.render(ShapeProperties.create(matrix, moduleX, moduleY, moduleWidth, moduleHeight)
                .round(5)
                .thickness(2)
                .outlineColor(new Color(55, 52, 55, 255).getRGB())
                .color(topLeftColor, topRightColor, bottomLeftColor, bottomRightColor)
                .build());

        rectangle.render(ShapeProperties.create(matrix, moduleX, moduleY + 30, moduleWidth, 1)
                .color(new Color(25, 25, 40, 155).getRGB(), new Color(55, 55, 60, 155).getRGB(),
                        new Color(55, 55, 60, 155).getRGB(), new Color(25, 25, 40, 155).getRGB())
                .build());

        Fonts.getSize(18, Fonts.Type.GUIICONS).drawString(matrix, "A", moduleX + 7, moduleY + 37f, new Color(128, 128, 128, 255).getRGB());
        Fonts.getSize(16, Fonts.Type.GUIICONS).drawString(matrix, "B", moduleX + 20, moduleY + 38f, new Color(128, 128, 128, 255).getRGB());

        renderStatusToggle(context, matrix, moduleX + moduleWidth - 16, moduleY + 35f, mouseX, mouseY);

        int alphaOffset = 150 + alphaAnimation.getOutput().intValue();
        Fonts.getSize(15, Fonts.Type.DEFAULT).drawString(matrix, "• Example Module", moduleX + 11, moduleY + 7f, new Color(255, 255, 255, alphaOffset).getRGB());

        Fonts.getSize(14, Fonts.Type.GUIICONS).drawString(matrix, "C", moduleX + 6.5f, moduleY + 19.5f, new Color(128, 128, 128, 255).getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(matrix, "Preview module description", moduleX + 15, moduleY + 19f, new Color(128, 128, 128, 186).getRGB());

        float bindX = moduleX + moduleWidth - 37.5f - 20;
        float bindY = moduleY + 34;

        rectangle.render(ShapeProperties.create(matrix, bindX + 7.85f, bindY, 18, 10)
                .round(3f)
                .outlineColor(new Color(155, 155, 165, 255).getRGB())
                .color(
                        new Color(61, 67, 71, 80).getRGB(),
                        new Color(71, 77, 81, 80).getRGB(),
                        new Color(81, 87, 91, 80).getRGB(),
                        new Color(91, 97, 101, 80).getRGB())
                .build());
        Fonts.getSize(22, GUIICONS).drawString(context.getMatrices(), "G", moduleX + moduleWidth - 46f, moduleY + 36, new Color(128, 128, 128, 255).getRGB());
     }

    private void renderStatusToggle(DrawContext context, MatrixStack matrix, float toggleX, float toggleY, int mouseX, int mouseY) {
        statusAlphaAnimation.setDirection(moduleEnabled ? FORWARDS : BACKWARDS);
        statusStencilAnimation.setDirection(moduleEnabled ? FORWARDS : BACKWARDS);
        statusSliderAnimation.setDirection(moduleEnabled ? FORWARDS : BACKWARDS);

        int stateColor = new Color(128, 128, 128, 255).getRGB();
        int opacity = statusAlphaAnimation.getOutput().intValue();
        float sliderX = toggleX - 8 + statusSliderAnimation.getOutput().floatValue();

        rectangle.render(ShapeProperties.create(matrix, toggleX - 8, toggleY, 16, 8)
                .round(4).thickness(0).softness(0)
                .outlineColor(new Color(128, 128, 128, 255).getRGB())
                .color(new Color(128, 128, 128, 40).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, toggleX - 8, toggleY, 16, 8)
                .round(4).thickness(0).softness(0)
                .outlineColor(new Color(128, 128, 128, 255).getRGB())
                .color(Calculate.applyOpacity(stateColor, opacity))
                .build());

        rectangle.render(ShapeProperties.create(matrix, sliderX - 0.5f, toggleY - 0.5f, 9, 9)
                .round(4.5f).thickness(2).softness(1)
                .outlineColor(new Color(155, 155, 165, 255).getRGB())
                .color(
                        new Color(61, 67, 71, 255).getRGB(),
                        new Color(71, 77, 81, 255).getRGB(),
                        new Color(81, 87, 91, 255).getRGB(),
                        new Color(91, 97, 101, 255).getRGB())
                .build());
    }

    private void renderExampleSettings(DrawContext context, MatrixStack matrix) {
        float settingsY = y + 90;
        float settingX = x + 10;
        float settingWidth = width - 20;

        rectangle.render(ShapeProperties.create(matrix, settingX, settingsY, settingWidth, 0.5f)
                .color(ThemeColorsGetter.getLines())
                .build());

        Fonts.getSize(14, Fonts.Type.DEFAULT).drawString(matrix, "Settings Preview", settingX + 5, settingsY + 8, ThemeColorsGetter.getText());

        float currentY = settingsY + 22;

        renderMultiSelectSetting(matrix, settingX, currentY, settingWidth, "Multi Select", "Option 1, Option 2");
        currentY += 22;

        renderSelectSetting(matrix, settingX, currentY, settingWidth, "Select", "Option 1");
        currentY += 20;

        renderSliderSetting(matrix, settingX, currentY, settingWidth, "Slider", 50f);
        currentY += 22;

        renderCheckboxSetting(matrix, settingX, currentY, settingWidth, "Checkbox", true);
        currentY += 18;

        renderBindSetting(matrix, settingX, currentY, settingWidth, "Bind", "LSHIFT");
        currentY += 17;

        renderColorSetting(matrix, settingX, currentY, settingWidth, "Color");
    }

    private void renderMultiSelectSetting(MatrixStack matrix, float x, float y, float width, String name, String selected) {
        Fonts.getSize(20, Fonts.Type.GUIICONS).drawString(matrix, "I", x + 6, y + 5f, new Color(128, 128, 128, 64).getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(matrix, name, x + 19, y + 6f, new Color(212, 214, 225, 255).getRGB());

        float selectX = x + width - 75;
        float selectY = y;

        rectangle.render(ShapeProperties.create(matrix, selectX, selectY, 66, 14)
                .round(3)
                .thickness(2)
                .outlineColor(new Color(35, 52, 55, 155).getRGB())
                .color(new Color(15, 15, 15, 0).getRGB())
                .build());

        Fonts.getSize(12, Fonts.Type.BOLD).drawString(matrix, selected, selectX + 3, selectY + 6, new Color(225, 225, 225, 225).getRGB());
    }

    private void renderSelectSetting(MatrixStack matrix, float x, float y, float width, String name, String selected) {
        Fonts.getSize(21, Fonts.Type.GUIICONS).drawString(matrix, "J", x + 6f, y + 4f, new Color(128, 128, 128, 64).getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(matrix, name, x + 19, y + 6f, new Color(212, 214, 225, 255).getRGB());

        float selectX = x + width - 75;
        float selectY = y;

        rectangle.render(ShapeProperties.create(matrix, selectX, selectY, 66, 14)
                .round(3)
                .thickness(2)
                .outlineColor(new Color(35, 52, 55, 155).getRGB())
                .color(new Color(15, 15, 15, 0).getRGB())
                .build());

        Fonts.getSize(12, Fonts.Type.BOLD).drawString(matrix, selected, selectX + 3, selectY + 6, new Color(225, 225, 225, 225).getRGB());
    }

    private void renderSliderSetting(MatrixStack matrix, float x, float y, float width, String name, float value) {
        Fonts.getSize(20, Fonts.Type.GUIICONS).drawString(matrix, "H", x + 6, y + 7.5f, new Color(128, 128, 128, 64).getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(matrix, name, x + 19, y + 8f, new Color(212, 214, 225, 255).getRGB());

        String valueText = String.valueOf((int) value);
        Fonts.getSize(12, Fonts.Type.BOLD).drawString(matrix, valueText, x + width - 9 - Fonts.getSize(12, Fonts.Type.BOLD).getStringWidth(valueText), y + 1, ThemeColorsGetter.getText());

        float sliderX = x + width - 74;
        float sliderY = y + 8;
        float sliderWidth = 65;
        float sliderHeight = 3;

        rectangle.render(ShapeProperties.create(matrix, sliderX, sliderY, sliderWidth, sliderHeight)
                .round(1)
                .color(new Color(45, 46, 65, 77).getRGB())
                .build());

        float fillWidth = (value / 100f) * sliderWidth;
        rectangle.render(ShapeProperties.create(matrix, sliderX, sliderY, fillWidth, sliderHeight)
                .round(1)
                .color(new Color(55, 55, 60, 155).getRGB(), new Color(155, 155, 160, 155).getRGB(),
                        new Color(255, 255, 255, 155).getRGB(), new Color(255, 255, 250, 155).getRGB())
                .build());

        float handleX = sliderX + fillWidth - 3.5f;
        rectangle.render(ShapeProperties.create(matrix, handleX, sliderY - 2, 7, 7)
                .round(3)
                .color(ColorAssist.getMainGuiColor())
                .build());

        rectangle.render(ShapeProperties.create(matrix, handleX + 0.5f, sliderY - 1.5f, 6, 6)
                .round(3)
                .thickness(2)
                .softness(0)
                .color(new Color(61, 67, 71, 255).getRGB(), new Color(71, 77, 81, 255).getRGB(),
                        new Color(81, 87, 91, 255).getRGB(), new Color(91, 97, 101, 255).getRGB())
                .build());
    }

    private void renderCheckboxSetting(MatrixStack matrix, float x, float y, float width, String name, boolean checked) {
        Fonts.getSize(20, Fonts.Type.GUIICONS).drawString(matrix, "K", x + 6, y + 3f, new Color(128, 128, 128, 64).getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(matrix, name, x + 20, y + 4f, new Color(212, 214, 225, 255).getRGB());

        float checkX = x + width - 19;
        float checkY = y - 1f;

        statusAlphaAnimation.setDirection(moduleEnabled ? FORWARDS : BACKWARDS);
        statusStencilAnimation.setDirection(moduleEnabled ? FORWARDS : BACKWARDS);
        statusSliderAnimation.setDirection(moduleEnabled ? FORWARDS : BACKWARDS);

        int stateColor = new Color(128, 128, 128, 255).getRGB();
        int opacity = statusAlphaAnimation.getOutput().intValue();
        float sliderX = checkX - 8 + statusSliderAnimation.getOutput().floatValue();

        rectangle.render(ShapeProperties.create(matrix, checkX - 8, checkY, 16, 8)
                .round(4).thickness(0).softness(0)
                .outlineColor(new Color(128, 128, 128, 255).getRGB())
                .color(new Color(128, 128, 128, 40).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, checkX - 8, checkY, 16, 8)
                .round(4).thickness(0).softness(0)
                .outlineColor(new Color(128, 128, 128, 255).getRGB())
                .color(Calculate.applyOpacity(stateColor, opacity))
                .build());

        rectangle.render(ShapeProperties.create(matrix, sliderX - 0.5f, checkY - 0.5f, 9, 9)
                .round(4.5f).thickness(2).softness(1)
                .outlineColor(new Color(155, 155, 165, 255).getRGB())
                .color(
                        new Color(61, 67, 71, 255).getRGB(),
                        new Color(71, 77, 81, 255).getRGB(),
                        new Color(81, 87, 91, 255).getRGB(),
                        new Color(91, 97, 101, 255).getRGB())
                .build());
    }

    private void renderBindSetting(MatrixStack matrix, float x, float y, float width, String name, String bind) {
        Fonts.getSize(14, Fonts.Type.GUIICONS).drawString(matrix, "L", x + 6, y + 4f, new Color(128, 128, 128, 64).getRGB());
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(matrix, name, x + 17, y + 3f, new Color(212, 214, 225, 255).getRGB());

        float stringWidth = Fonts.getSize(11, Fonts.Type.SEMI).getStringWidth(bind) - 2;
        float bindX = x + width - stringWidth - 19;
        float bindY = y - 1.5f;


        rectangle.render(ShapeProperties.create(matrix, x + width - stringWidth - 19, y - 2.5f, stringWidth + 10, 11.5f)
                .round(3).softness(1).thickness(2).outlineColor(new Color(55,52,55,255).getRGB())
                .color(
                        new Color(25,22,25,0).getRGB(),
                        new Color(31,27,35,0).getRGB(),
                        new Color(31,27,35,0).getRGB(),
                        new Color(25,22,25,0).getRGB())
                .build());
        Fonts.getSize(11, Fonts.Type.SEMI).drawString(matrix, bind, bindX + 4, bindY + 4, new Color(135, 136, 148, 255).getRGB());
    }

    private void renderColorSetting(MatrixStack matrix, float x, float y, float width, String name) {
        Fonts.getSize(16, Fonts.Type.ICONSCATEGORY).drawString(matrix, "G", x + 6, y + 4.5f, new Color(128, 128, 128, 64).getRGB());
        Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(matrix, name, x + 17, y + 5f, new Color(212, 214, 225, 255).getRGB());

        float colorX = x + width - 18;
        float colorY = y + 3;

        rectangle.render(ShapeProperties.create(matrix, colorX, colorY, 7, 7)
                .round(3.5f)
                .color(ThemeColorsGetter.getPrimary())
                .build());

        rectangle.render(ShapeProperties.create(matrix, colorX, colorY, 7, 7)
                .round(3.5f)
                .thickness(2)
                .softness(1)
                .outlineColor(ThemeColorsGetter.getText())
                .color(0x00FFFFFF)
                .build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (Calculate.isHovered(mouseX, mouseY, x + width - 25, y + 5, 20, 20)) {
                startCloseAnimation();
                return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, x, y, width, 19)) {
                dragging = true;
                dragX = (int) (x - mouseX);
                dragY = (int) (y - mouseY);
                return true;
            }

            float moduleX = x + 10;
            float moduleY = y + 35;
            float moduleWidth = width - 20;
            float toggleX = moduleX + moduleWidth - 16;
            float toggleY = moduleY + 56f;

            if (Calculate.isHovered(mouseX, mouseY, toggleX - 8, toggleY, 16, 8)) {
                moduleEnabled = !moduleEnabled;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return super.isHovered(mouseX, mouseY);
    }
}