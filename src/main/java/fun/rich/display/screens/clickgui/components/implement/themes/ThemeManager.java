package fun.rich.display.screens.clickgui.components.implement.themes;

import fun.rich.features.module.setting.implement.ColorSetting;
import fun.rich.utils.display.color.ColorAssist;
import lombok.Getter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ThemeManager {
    private static ThemeManager instance;

    private final List<ColorSetting> interfaceColors = new ArrayList<>();
    private final List<ColorSetting> clickGuiColors = new ArrayList<>();

    private ThemeManager() {
        initializeInterfaceColors();
        initializeClickGuiColors();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    private void initializeInterfaceColors() {
        interfaceColors.add(new ColorSetting("Primary", "Primary interface color").value(0xFF6565FF));
        interfaceColors.add(new ColorSetting("Secondary", "Secondary interface color").value(0xFF8B8BFF));
        interfaceColors.add(new ColorSetting("Accent", "Accent color").value(0xFFFF6539));
        interfaceColors.add(new ColorSetting("Text", "Text color").value(0xFFD4D6E1));
        interfaceColors.add(new ColorSetting("TextDim", "Dimmed text color").value(0xFF878894));
        interfaceColors.add(new ColorSetting("Background", "Background color").value(0xFF121314));
    }

    private void initializeClickGuiColors() {
        clickGuiColors.add(new ColorSetting("Background", "Background").value(new Color(18, 19, 20, 175).getRGB()));
        clickGuiColors.add(new ColorSetting("Lines", "Lines").value(new Color(55, 55, 70, 250).getRGB()));
        clickGuiColors.add(new ColorSetting("Text", "Text").value(new Color(255, 255, 255, 185).getRGB()));
    }

    public ColorSetting getColor(String name) {
        for (ColorSetting color : interfaceColors) {
            if (color.getName().equals(name)) return color;
        }
        for (ColorSetting color : clickGuiColors) {
            if (color.getName().equals(name)) return color;
        }
        return null;
    }

    public List<ColorSetting> getAllColors() {
        List<ColorSetting> all = new ArrayList<>();
        all.addAll(interfaceColors);
        all.addAll(clickGuiColors);
        return all;
    }

    public int getPrimary() {
        ColorSetting color = getColor("Primary");
        return color != null ? color.getColor() : 0xFF6565FF;
    }

    public int getSecondary() {
        ColorSetting color = getColor("Secondary");
        return color != null ? color.getColor() : 0xFF8B8BFF;
    }

    public int getAccent() {
        ColorSetting color = getColor("Accent");
        return color != null ? color.getColor() : 0xFFFF6539;
    }

    public int getText() {
        ColorSetting color = getColor("Text");
        return color != null ? color.getColor() : 0xFFD4D6E1;
    }

    public int getTextDim() {
        ColorSetting color = getColor("TextDim");
        return color != null ? color.getColor() : 0xFF878894;
    }

    public int getBackground() {
        ColorSetting interfaceColor = interfaceColors.stream()
                .filter(c -> c.getName().equals("Background"))
                .findFirst()
                .orElse(null);
        return interfaceColor != null ? interfaceColor.getColor() : 0xFF121314;
    }

    public int getGuiBackground() {
        ColorSetting guiColor = clickGuiColors.stream()
                .filter(c -> c.getName().equals("Background"))
                .findFirst()
                .orElse(null);
        return guiColor != null ? guiColor.getColor() : new Color(18, 19, 20, 255).getRGB();
    }

    public int getLines() {
        ColorSetting color = getColor("Lines");
        return color != null ? color.getColor() : new Color(55, 55, 70, 250).getRGB();
    }

    public int getHover() {
        ColorSetting color = getColor("Hover");
        return color != null ? color.getColor() : 0xFF414141;
    }

    public int getTextWithAlpha(float alpha) {
        int baseColor = getText();
        int a = (int) (alpha * 255);
        return (baseColor & 0x00FFFFFF) | (a << 24);
    }

    public int getTextDimWithAlpha(float alpha) {
        int baseColor = getTextDim();
        int a = (int) (alpha * 255);
        return (baseColor & 0x00FFFFFF) | (a << 24);
    }
}