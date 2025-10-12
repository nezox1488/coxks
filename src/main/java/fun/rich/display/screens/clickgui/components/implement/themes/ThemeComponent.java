package fun.rich.display.screens.clickgui.components.implement.themes;

import fun.rich.utils.client.managers.file.impl.ThemeFile;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.MinecraftClient;
import fun.rich.features.module.ModuleCategory;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.display.screens.clickgui.MenuScreen;
import fun.rich.display.screens.clickgui.components.AbstractComponent;
import fun.rich.Rich;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.display.screens.clickgui.components.implement.window.AbstractWindow;
import fun.rich.display.screens.clickgui.components.implement.window.implement.settings.color.ColorWindow;
import fun.rich.features.module.setting.implement.ColorSetting;
import fun.rich.utils.display.scissor.ScissorAssist;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Setter
@Accessors(chain = true)
public class ThemeComponent extends AbstractComponent {
    private final ThemeManager themeManager = ThemeManager.getInstance();
    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private String themeName = "";
    private boolean editingName = false;
    private int nameCursor = 0;
    private List<String> themeList = new ArrayList<>();
    private boolean dropdownOpen = false;
    private int selectedThemeIndex = 0;
    private float dropdownScroll = 0f;
    private boolean previewEnabled = false;
    private PreviewWindow previewWindow = null;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (MenuScreen.INSTANCE.getCategory() != ModuleCategory.THEME) {
            return;
        }

        MatrixStack matrix = context.getMatrices();
        loadThemeList();

        renderControls(context, matrix, mouseX, mouseY);

        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
        ScissorAssist scissorManager = Rich.getInstance().getScissorManager();
        float listX = x + 43f;
        float listY = y + 95f;
        float listWidth = width - 43f - 15f;
        float listHeight = height - 95.5f;

        scissorManager.push(positionMatrix, listX, listY, listWidth, listHeight);

        float currentY = y + 105 + smoothedScroll;
        float sectionSpacing = 35f;

        drawSection(context, "Interface", themeManager.getInterfaceColors(), currentY, mouseX, mouseY);
        currentY += sectionSpacing + themeManager.getInterfaceColors().size() * 18f + 8f;

        drawSection(context, "ClickGui", themeManager.getClickGuiColors(), currentY, mouseX, mouseY);

        scissorManager.pop();

        float contentHeight = sectionSpacing * 2 +
                themeManager.getInterfaceColors().size() * 18f + 8f +
                themeManager.getClickGuiColors().size() * 18f + 8f;

        float viewHeight = height - 110f;
        float maxScrollAmount = Math.max(0f, contentHeight - viewHeight) - 30;

        scroll = net.minecraft.util.math.MathHelper.clamp(scroll, -maxScrollAmount, 0f);
        smoothedScroll = Calculate.interpolate(smoothedScroll, scroll, 0.2f);

        if (maxScrollAmount > 0) {
            drawScrollbar(context, viewHeight, contentHeight, maxScrollAmount);
        }

        if (dropdownOpen) {
            renderDropdown(context, matrix, mouseX, mouseY);
        }
    }

    private void renderControls(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        rectangle.render(ShapeProperties.create(matrix, x + 43F, y + 60, width - 43F, 0.5F)
                .color(ThemeColorsGetter.getLines(), ThemeColorsGetter.getLines(), ThemeColorsGetter.getLines(), ThemeColorsGetter.getLines()).build());

        rectangle.render(ShapeProperties.create(matrix, x + 55F, y + 38, 200, 15)
                .round(3).thickness(2).softness(1).outlineColor(new Color(54, 54, 56, 255).getRGB()).color(
                        new Color(31, 27, 35, 75).getRGB()).build());

        String placeholder = "Имя темы";
        String displayText = (themeName.isEmpty() && !editingName) ? placeholder : themeName;
        Fonts.getSize(15, Fonts.Type.REGULAR).drawString(matrix, displayText, x + 58F, y + 43.5f, ColorAssist.getText(editingName ? 1f : 0.6f));

        if (editingName && System.currentTimeMillis() % 1000 < 500) {
            float curWidth = Fonts.getSize(15, Fonts.Type.REGULAR).getStringWidth(themeName.substring(0, nameCursor));
            Fonts.getSize(15, Fonts.Type.DEFAULT).drawString(matrix, "|", x + 57F + curWidth, y + 43f, ColorAssist.getText(0.7f));
        }

        rectangle.render(ShapeProperties.create(matrix, x + 262F, y + 38, 55, 15)
                .round(3).thickness(2).softness(1).outlineColor(new Color(54, 54, 56, 255).getRGB()).color(
                        new Color(31, 27, 35, 75).getRGB()).build());
        Fonts.getSize(22, Fonts.Type.GUIICONS).drawString(matrix, "M", x + 266F, y + 42f, ColorAssist.getText(1f));
        Fonts.getSize(16, Fonts.Type.REGULAR).drawString(matrix, "Save", x + 283F, y + 43.5f, ColorAssist.getText(1f));

        rectangle.render(ShapeProperties.create(matrix, x + 55F, y + 70, 140, 15)
                .round(3).thickness(2).softness(1).outlineColor(new Color(54, 54, 56, 255).getRGB()).color(
                        new Color(31, 27, 35, 75).getRGB()).build());

        String selectedTheme = themeList.isEmpty() ? "Нет тем" : (selectedThemeIndex < themeList.size() ? themeList.get(selectedThemeIndex) : "Нет тем");
        String displayTheme = selectedTheme.length() > 18 ? selectedTheme.substring(0, 18) : selectedTheme;
        Fonts.getSize(15, Fonts.Type.REGULAR).drawString(matrix, displayTheme, x + 58F, y + 75.5f, ColorAssist.getText(0.6f));
        Fonts.getSize(12, Fonts.Type.REGULAR).drawString(matrix, dropdownOpen ? "opened" : "closed", x + 169F, y + 76.5f, ColorAssist.getText(0.15f));

        rectangle.render(ShapeProperties.create(matrix, x + 202F, y + 70, 55, 15)
                .round(3).thickness(2).softness(1).outlineColor(new Color(54, 54, 56, 255).getRGB()).color(
                        new Color(31, 27, 35, 75).getRGB()).build());
        Fonts.getSize(26, Fonts.Type.GUIICONS).drawString(matrix, "P", x + 206F, y + 73f, ColorAssist.getText(1f));
        Fonts.getSize(16, Fonts.Type.REGULAR).drawString(matrix, "Load", x + 222.5F, y + 75.5f, ColorAssist.getText(1f));

        rectangle.render(ShapeProperties.create(matrix, x + 262F, y + 70, 55, 15)
                .round(3).thickness(2).softness(1).outlineColor(new Color(54, 54, 56, 255).getRGB()).color(
                        new Color(31, 27, 35, 75).getRGB()).build());
        Fonts.getSize(21, Fonts.Type.GUIICONS).drawString(matrix, "O", x + 266F, y + 74.5f, ColorAssist.getText(1f));
        Fonts.getSize(16, Fonts.Type.REGULAR).drawString(matrix, "Delete", x + 280F, y + 75.5f, ColorAssist.getText(1f));

        rectangle.render(ShapeProperties.create(matrix, x + 43F, y + 94f, width - 43F, 1F)
                .color(ThemeColorsGetter.getLines(), ThemeColorsGetter.getLines(), ThemeColorsGetter.getLines(), ThemeColorsGetter.getLines()).build());
    }

    private void renderDropdown(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        if (themeList.isEmpty()) return;

        int maxVisible = Math.min(5, themeList.size());
        float dropdownHeight = maxVisible * 20f;

        blur.render(ShapeProperties.create(matrix, x + 55F, y + 87, 140, dropdownHeight + 4).round(4).quality(16)
                .color(new Color(0, 0, 0, 200).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, x + 55F, y + 87, 140, dropdownHeight + 4)
                .round(4).thickness(2).softness(1).outlineColor(new Color(54, 54, 56, 255).getRGB()).color(
                        new Color(31, 27, 35, 75).getRGB()).build());

        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
        ScissorAssist scissorManager = Rich.getInstance().getScissorManager();
        scissorManager.push(positionMatrix, x + 55F, y + 89, 140, dropdownHeight);

        float itemY = y + 90 + dropdownScroll;
        for (int i = 0; i < themeList.size(); i++) {
            String theme = themeList.get(i);
            boolean hovered = Calculate.isHovered(mouseX, mouseY, x + 55F, itemY, 140, 20);
            boolean selected = i == selectedThemeIndex;

            if (hovered || selected) {
                rectangle.render(ShapeProperties.create(matrix, x + 57F, itemY, 131, 18)
                        .round(3).color(new Color(35, 35, 37, 155).getRGB()).build());
            }

            String displayTheme = theme.length() > 18 ? theme.substring(0, 18) : theme;
            Fonts.getSize(14, Fonts.Type.DEFAULT).drawString(matrix, displayTheme, x + 62F, itemY + 7.5f,
                    selected ? ColorAssist.getText(1f) : ColorAssist.getText(0.7f));

            itemY += 20;
        }

        scissorManager.pop();

        if (themeList.size() > maxVisible) {
            float scrollbarHeight = dropdownHeight * ((float)maxVisible / themeList.size());
            float maxScroll = (themeList.size() - maxVisible) * 20f;
            float scrollRatio = maxScroll > 0 ? (-dropdownScroll) / maxScroll : 0;
            float scrollbarY = y + 89 + (dropdownHeight - scrollbarHeight) * scrollRatio;

            rectangle.render(ShapeProperties.create(matrix, x + 190F, scrollbarY + 1.5f, 2, scrollbarHeight - 3)
                    .round(1).color(new Color(100, 100, 100, 150).getRGB()).build());
        }
    }

    private void drawSection(DrawContext context, String title, List<ColorSetting> colors, float startY, int mouseX, int mouseY) {
        MatrixStack matrix = context.getMatrices();

        rectangle.render(ShapeProperties.create(matrix, x + 43F, startY - 12, width - 43F, 0.5F)
                .color(ThemeColorsGetter.getLines(), ThemeColorsGetter.getLines(), ThemeColorsGetter.getLines(), ThemeColorsGetter.getLines()).build());

        Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(matrix, title, x + 58F, startY, ColorAssist.getText(1f));

        float sectionHeight = colors.size() * 19;

        rectangle.render(ShapeProperties.create(matrix, x + 55f, startY + 10f, 365, sectionHeight)
                .round(5).thickness(2).softness(1)
                .outlineColor(new Color(54, 54, 56, 255).getRGB())
                .color(new Color(31, 27, 35, 75).getRGB()).build());

        float colorY = startY + 14f;

        for (ColorSetting color : colors) {
            drawColorBox(context, color, colorY, mouseX, mouseY);
            colorY += 18f;
        }
    }

    private void drawColorBox(DrawContext context, ColorSetting color, float boxY, int mouseX, int mouseY) {
        MatrixStack matrix = context.getMatrices();
        float boxX = x + 60f;

        String name = color.getName();
        Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(matrix, name, boxX, boxY + 5, ColorAssist.getText(0.7f));

        rectangle.render(ShapeProperties.create(matrix, boxX + 340, boxY + 2, 12, 12)
                .round(6).color(color.getColor()).build());

        rectangle.render(ShapeProperties.create(matrix, boxX + 340, boxY + 2, 12, 12)
                .round(6).thickness(2.5f).softness(1)
                .outlineColor(ColorAssist.getText()).color(0x0FFFFFF).build());

//        rectangle.render(ShapeProperties.create(matrix, boxX + 323, boxY + 2, 14, 12)
//                .round(3).thickness(1.5f).softness(1)
//                .outlineColor(new Color(54, 54, 56, 255).getRGB())
//                .color(new Color(31, 27, 35, 100).getRGB()).build());

//        Fonts.getSize(18, Fonts.Type.ICONS).drawString(matrix, "D", boxX + 325.5f, boxY + 4.5f, ColorAssist.getText(0.8f));
    }

    private void drawScrollbar(DrawContext context, float viewHeight, float contentHeight, float maxScrollAmount) {
        float scrollbarWidth = 4;
        float scrollbarX = x + width - 10;
        float scrollbarY = y + 105;
        float scrollbarHeight = height - 110;

        rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight)
                .round(2).color(new Color(30, 30, 30, 100).getRGB()).build());

        float handleHeight = Math.max(20, scrollbarHeight * (viewHeight / contentHeight));
        float scrollRatio = maxScrollAmount > 0 ? (-smoothedScroll) / maxScrollAmount : 0;
        float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;

        rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, handleY, scrollbarWidth, handleHeight)
                .round(2).color(new Color(100, 100, 100, 150).getRGB()).build());
    }

    private void loadThemeList() {
        themeList.clear();
        File dir = Rich.getInstance().getClientInfoProvider().filesDir();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith("theme_") && f.getName().endsWith(".json")) {
                    String name = f.getName().replace("theme_", "").replace(".json", "");
                    themeList.add(name);
                }
            }
        }
        if (selectedThemeIndex >= themeList.size()) {
            selectedThemeIndex = Math.max(0, themeList.size() - 1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MenuScreen.INSTANCE.getCategory() != ModuleCategory.THEME) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0) {
            if (!Calculate.isHovered(mouseX, mouseY, x + 55, y + 38, 200, 15)) {
                editingName = false;
            }

            if (dropdownOpen && !Calculate.isHovered(mouseX, mouseY, x + 55F, y + 70, 140, 15)
                    && !Calculate.isHovered(mouseX, mouseY, x + 55F, y + 87, 140, Math.min(5, themeList.size()) * 20f + 4)) {
                dropdownOpen = false;
                return true;
            }
        }

        if (button == 0) {
            if (Calculate.isHovered(mouseX, mouseY, x + 55, y + 38, 200, 15)) {
                editingName = true;
                nameCursor = themeName.length();
                dropdownOpen = false;
                return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, x + 55F, y + 70, 140, 15)) {
                dropdownOpen = !dropdownOpen;
                return true;
            }

            if (dropdownOpen && Calculate.isHovered(mouseX, mouseY, x + 55F, y + 87, 140, Math.min(5, themeList.size()) * 20f + 4)) {
                float itemY = y + 89 + dropdownScroll;
                for (int i = 0; i < themeList.size(); i++) {
                    if (Calculate.isHovered(mouseX, mouseY, x + 55F, itemY, 140, 20)) {
                        selectedThemeIndex = i;
                        dropdownOpen = false;
                        return true;
                    }
                    itemY += 20;
                }
                return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, x + 262, y + 38, 55, 15)) {
                saveTheme();
                dropdownOpen = false;
                return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, x + 202, y + 70, 55, 15)) {
                loadTheme();
                dropdownOpen = false;
                return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, x + 262, y + 70, 55, 15)) {
                deleteTheme();
                dropdownOpen = false;
                return true;
            }

            if (!dropdownOpen) {
                float currentY = y + 105 + smoothedScroll;

                if (checkColorClicks(themeManager.getInterfaceColors(), currentY + 5, mouseX, mouseY)) {
                    return true;
                }

                currentY += 35f + themeManager.getInterfaceColors().size() * 18f + 8f;

                if (checkColorClicks(themeManager.getClickGuiColors(), currentY + 5, mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void saveTheme() {
        if (themeName.isEmpty()) return;
        try {
            ThemeFile themeFile = new ThemeFile(themeManager);
            themeFile.saveToFile(Rich.getInstance().getClientInfoProvider().filesDir(), "theme_" + themeName + ".json");
            themeName = "";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTheme() {
        if (themeList.isEmpty() || selectedThemeIndex >= themeList.size()) return;
        String name = themeList.get(selectedThemeIndex);
        try {
            ThemeFile themeFile = new ThemeFile(themeManager);
            themeFile.loadFromFile(Rich.getInstance().getClientInfoProvider().filesDir(), "theme_" + name + ".json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteTheme() {
        if (themeList.isEmpty() || selectedThemeIndex >= themeList.size()) return;
        String name = themeList.get(selectedThemeIndex);
        File file = new File(Rich.getInstance().getClientInfoProvider().filesDir(), "theme_" + name + ".json");
        file.delete();
        selectedThemeIndex = Math.max(0, selectedThemeIndex - 1);
    }

    private boolean checkColorClicks(List<ColorSetting> colors, float startY, double mouseX, double mouseY) {
        float colorY = startY + 14f;

        for (ColorSetting color : colors) {
            if (Calculate.isHovered(mouseX, mouseY, x + 60f + 340, colorY + 2, 12, 12)) {
                openColorWindow(color, mouseX, mouseY);
                return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, x + 60f + 323, colorY + 2, 14, 12)) {
                togglePreview();
                return true;
            }

            colorY += 18f;
        }
        return false;
    }

    private void togglePreview() {
        previewEnabled = !previewEnabled;

        if (previewEnabled) {
            if (previewWindow == null || !windowManager.getWindows().contains(previewWindow)) {
                previewWindow = new PreviewWindow();
                previewWindow.position(MenuScreen.INSTANCE.x - 240, MenuScreen.INSTANCE.y - 15)
                        .size(200, 315)
                        .draggable(true);
                windowManager.add(previewWindow);
            }
        } else {
            if (previewWindow != null) {
                windowManager.delete(previewWindow);
                previewWindow = null;
            }
        }
    }

    private void openColorWindow(ColorSetting setting, double mouseX, double mouseY) {
        AbstractWindow existingWindow = null;

        for (AbstractWindow window : windowManager.getWindows()) {
            if (window instanceof ColorWindow) {
                existingWindow = window;
                break;
            }
        }

        if (existingWindow != null) {
            windowManager.delete(existingWindow);
        } else {
            AbstractWindow colorWindow = new ColorWindow(setting)
                    .position((int) (mouseX - 110), (int) (mouseY - 20))
                    .size(100, 155)
                    .draggable(true);

            windowManager.add(colorWindow);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (MenuScreen.INSTANCE.getCategory() == ModuleCategory.THEME) {
            if (dropdownOpen && Calculate.isHovered(mouseX, mouseY, x + 55F, y + 87, 140, Math.min(5, themeList.size()) * 20f + 4)) {
                int maxVisible = 5;
                float maxScroll = Math.max(0, (themeList.size() - maxVisible) * 20f);
                dropdownScroll += amount * 10;
                dropdownScroll = net.minecraft.util.math.MathHelper.clamp(dropdownScroll, -maxScroll, 0);
                return true;
            }

            if (Calculate.isHovered(mouseX, mouseY, x + 43, y + 105, width - 43 - 15, height - 110)) {
                scroll += amount * 20;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingName) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingName = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (nameCursor > 0) {
                    themeName = themeName.substring(0, nameCursor - 1) + themeName.substring(nameCursor);
                    nameCursor--;
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
                if (nameCursor > 0) nameCursor--;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                if (nameCursor < themeName.length()) nameCursor++;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clip = MinecraftClient.getInstance().keyboard.getClipboard().trim();
                int avail = 20 - themeName.length();
                if (clip.length() > avail) clip = clip.substring(0, avail);
                themeName = themeName.substring(0, nameCursor) + clip + themeName.substring(nameCursor);
                nameCursor += clip.length();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingName && themeName.length() < 20 && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-')) {
            themeName = themeName.substring(0, nameCursor) + chr + themeName.substring(nameCursor);
            nameCursor++;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}