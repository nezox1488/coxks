package fun.rich.display.screens.clickgui.components.implement.themes;

import fun.rich.features.module.setting.implement.ColorSetting;
import lombok.Getter;

import java.awt.*;
import java.util.*;
import java.util.List;

@Getter
public class ThemeManager {
    private static ThemeManager instance;

    private final Map<ThemeColor, ColorSetting> colorMap = new HashMap<>();
    private final List<ColorSetting> interfaceColors = new ArrayList<>();
    private final List<ColorSetting> clickGuiColors = new ArrayList<>();

    private ThemeManager() {
        initializeColors();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    private void initializeColors() {
        registerInterfaceColor(ThemeColor.PRIMARY, "Primary", "Primary interface color",
                new Color(101, 101, 255, 255));
        registerInterfaceColor(ThemeColor.SECONDARY, "Secondary", "Secondary interface color",
                new Color(139, 139, 255, 255));
        registerInterfaceColor(ThemeColor.ACCENT, "Accent", "Accent color",
                new Color(255, 101, 57, 255));
        registerInterfaceColor(ThemeColor.TEXT, "Text", "Text color",
                new Color(212, 214, 225, 255));
        registerInterfaceColor(ThemeColor.TEXT_DIM, "Text Dim", "Dimmed text color",
                new Color(135, 136, 148, 255));
        registerInterfaceColor(ThemeColor.BACKGROUND, "Background", "Background color",
                new Color(18, 19, 20, 255));

        registerClickGuiColor(ThemeColor.GUI_BACKGROUND, "GUI Background", "GUI Background",
                new Color(18, 19, 20, 175));
        registerClickGuiColor(ThemeColor.LINES, "Lines", "Lines color",
                new Color(55, 55, 70, 250));
        registerClickGuiColor(ThemeColor.GUI_TEXT, "GUI Text", "GUI text color",
                new Color(255, 255, 255, 185));
        registerClickGuiColor(ThemeColor.HOVER, "Hover", "Hover color",
                new Color(65, 65, 65, 255));
        registerClickGuiColor(ThemeColor.ENABLED_MODULE, "Enabled Module", "Enabled module color",
                new Color(19, 19, 21, 255));
        registerClickGuiColor(ThemeColor.DISABLED_MODULE, "Disabled Module", "Disabled module color",
                new Color(19, 19, 21, 0));
    }

    private void registerInterfaceColor(ThemeColor type, String name, String description, Color defaultColor) {
        ColorSetting setting = new ColorSetting(name, description).value(defaultColor.getRGB());
        colorMap.put(type, setting);
        interfaceColors.add(setting);
    }

    private void registerClickGuiColor(ThemeColor type, String name, String description, Color defaultColor) {
        ColorSetting setting = new ColorSetting(name, description).value(defaultColor.getRGB());
        colorMap.put(type, setting);
        clickGuiColors.add(setting);
    }

    public int getColor(ThemeColor type) {
        ColorSetting setting = colorMap.get(type);
        return setting != null ? setting.getColor() : new Color(255, 0, 255, 255).getRGB();
    }

    public int getColorWithAlpha(ThemeColor type, float alpha) {
        int baseColor = getColor(type);
        int a = (int) (alpha * 255);
        return (baseColor & 0x00FFFFFF) | (a << 24);
    }

    public ColorSetting getColorSetting(ThemeColor type) {
        return colorMap.get(type);
    }

    public List<ColorSetting> getAllColors() {
        List<ColorSetting> all = new ArrayList<>();
        all.addAll(interfaceColors);
        all.addAll(clickGuiColors);
        return all;
    }

    public int getPrimary() {
        return getColor(ThemeColor.PRIMARY);
    }

    public int getSecondary() {
        return getColor(ThemeColor.SECONDARY);
    }

    public int getAccent() {
        return getColor(ThemeColor.ACCENT);
    }

    public int getText() {
        return getColor(ThemeColor.TEXT);
    }

    public int getTextDim() {
        return getColor(ThemeColor.TEXT_DIM);
    }

    public int getBackground() {
        return getColor(ThemeColor.BACKGROUND);
    }

    public int getGuiBackground() {
        return getColor(ThemeColor.GUI_BACKGROUND);
    }

    public int getLines() {
        return getColor(ThemeColor.LINES);
    }

    public int getGuiText() {
        return getColor(ThemeColor.GUI_TEXT);
    }

    public int getHover() {
        return getColor(ThemeColor.HOVER);
    }

    public int getEnabledModule() {
        return getColor(ThemeColor.ENABLED_MODULE);
    }

    public int getDisabledModule() {
        return getColor(ThemeColor.DISABLED_MODULE);
    }

    public int getTextWithAlpha(float alpha) {
        return getColorWithAlpha(ThemeColor.TEXT, alpha);
    }

    public int getTextDimWithAlpha(float alpha) {
        return getColorWithAlpha(ThemeColor.TEXT_DIM, alpha);
    }

    public int getGuiBackgroundWithAlpha(float alpha) {
        return getColorWithAlpha(ThemeColor.GUI_BACKGROUND, alpha);
    }

    public int getPrimaryWithAlpha(float alpha) {
        return getColorWithAlpha(ThemeColor.PRIMARY, alpha);
    }

    public int getSecondaryWithAlpha(float alpha) {
        return getColorWithAlpha(ThemeColor.SECONDARY, alpha);
    }

    public int getAccentWithAlpha(float alpha) {
        return getColorWithAlpha(ThemeColor.ACCENT, alpha);
    }

    public int getEnabledModuleWithAlpha(float alpha) {
        return getColorWithAlpha(ThemeColor.ENABLED_MODULE, alpha);
    }

    public int getDisabledModuleWithAlpha(float alpha) {
        return getColorWithAlpha(ThemeColor.DISABLED_MODULE, alpha);
    }

    public enum ThemeColor {
        PRIMARY,
        SECONDARY,
        ACCENT,
        TEXT,
        TEXT_DIM,
        BACKGROUND,
        GUI_BACKGROUND,
        LINES,
        GUI_TEXT,
        HOVER,
        ENABLED_MODULE,
        DISABLED_MODULE
    }
}