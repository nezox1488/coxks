package fun.rich.display.screens.clickgui.components.implement.themes;

import fun.rich.features.module.setting.implement.ColorSetting;
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
        clickGuiColors.add(new ColorSetting("Background", "Background").value(new Color(18, 19, 20, 255).getRGB()));
        clickGuiColors.add(new ColorSetting("Outline", "Outline").value(0xFF363638));
        clickGuiColors.add(new ColorSetting("Hover", "Hover effect").value(0xFF414141));
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
}