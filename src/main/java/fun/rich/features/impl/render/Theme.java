package fun.rich.features.impl.render;
/**
 * @author Sitoku
 * @since 3/3/2026
 */
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.features.module.setting.implement.ColorSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.client.Instance;

import java.awt.*;

public class Theme extends Module {

    public static Theme getInstance() {
        return Instance.get(Theme.class);
    }

    public ColorSetting clientColor = new ColorSetting("Клиентский цвет 1", "Первый цвет полоски HP и разделителя (переливание)")
            .setColor(new Color(30, 30, 30, 255).getRGB()).presets(0xFF6C9AFD, 0xFF8C7FFF);
    public ColorSetting clientColor2 = new ColorSetting("Клиентский цвет 2", "Второй цвет полоски HP и разделителя (переливание)")
            .setColor(new Color(120, 120, 120, 255).getRGB()).presets(0xFF8C7FFF, 0xFF6C9AFD);

    public ColorSetting targetEspColor = new ColorSetting("Цвет таргет ESP", "Цвет Target ESP")
            .setColor(new Color(255, 101, 57, 255).getRGB());

    public ColorSetting guiBgColor = new ColorSetting("Цвет фона GUI", "Фон кликгуи")
            .setColor(new Color(18, 19, 20, 200).getRGB());

    public ColorSetting hotkeysBgColor = new ColorSetting("Цвет фона хоткеев", "Фон блока хоткеев")
            .setColor(new Color(18, 19, 20, 150).getRGB());

    public ColorSetting potionsBgColor = new ColorSetting("Цвет фона потионсов", "Фон блока потионсов")
            .setColor(new Color(18, 19, 20, 150).getRGB());

    public ColorSetting watermarkBgColor = new ColorSetting("Цвет фона ватермарки", "Фон ватермарки")
            .setColor(new Color(18, 19, 20, 150).getRGB());

    public SliderSettings watermarkBgAlpha = new SliderSettings("Прозрачность фона ватермарки", "Прозрачность")
            .range(0f, 255f).setValue(150f);

    public BooleanSetting watermarkBlur = new BooleanSetting("Блюр ватермарки", "Включить размытие фона ватермарки")
            .setValue(true);

    public Theme() {
        super("Theme", "Theme", ModuleCategory.THEME);
        setup(clientColor, clientColor2, targetEspColor, guiBgColor, hotkeysBgColor, potionsBgColor, watermarkBgColor, watermarkBgAlpha, watermarkBlur);
    }
}
