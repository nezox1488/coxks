package fun.rich.features.impl.render;

import fun.rich.features.module.setting.implement.BooleanSetting;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.features.module.setting.implement.ColorSetting;
import fun.rich.features.module.setting.implement.MultiSelectSetting;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.utils.client.Instance;
import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends Module {
    public static Hud getInstance() { return Instance.get(Hud.class); }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Элементы", "Настройка элементов интерфейса")
            .value("Watermark", "Hot Keys", "Target Hud", "Armor Hud", "Notifications", "Staff List", "Potions")
            .selected("Watermark", "Hot Keys", "Target Hud", "Armor Hud", "Notifications", "Staff List", "Potions");

    public SliderSettings opacityWatermark = new SliderSettings("Прозрачность: Watermark", "").range(0f, 255f).setValue(200f);
    public SliderSettings opacityHotKeys = new SliderSettings("Прозрачность: Hot Keys", "").range(0f, 255f).setValue(200f);
    public SliderSettings opacityTargetHud = new SliderSettings("Прозрачность: Target Hud", "").range(0f, 255f).setValue(200f);
    public SliderSettings opacityNotifications = new SliderSettings("Прозрачность: Notifications", "").range(0f, 255f).setValue(200f);
    public SliderSettings opacityStaffList = new SliderSettings("Прозрачность: Staff List", "").range(0f, 255f).setValue(200f);
    public SliderSettings opacityPotions = new SliderSettings("Прозрачность: Potions", "").range(0f, 255f).setValue(200f);
    public SliderSettings opacityArmorHud = new SliderSettings("Прозрачность: Armor Hud", "").range(0f, 255f).setValue(200f);

    public MultiSelectSetting notificationSettings = new MultiSelectSetting("Уведомления", "Выберите типы уведомлений")
            .value("Module Switch", "Staff Join", "Staff Leave", "Item Pick Up", "Auto Armor", "Break Shield", "Low Armor Durability", "Totem Loss", "Gapple")
            .selected("Module Switch", "Item Pick Up", "Auto Armor", "Break Shield", "Low Armor Durability", "Totem Loss", "Gapple");

    public SliderSettings soundVolumeSetting = new SliderSettings("Sound Volume", "Громкость звуков")
            .range(0.0f, 1.0f)
            .setValue(1.0f);
    public float getModuleVolume() {
        return soundVolumeSetting.getValue();
    }

    public BooleanSetting khBlur = new BooleanSetting("Хоткеи: Блюр", "").setValue(true);
    public SliderSettings khBlurAmount = new SliderSettings("Хоткеи: Сила блюра", "").range(1.0f, 15.0f).setValue(5.0f);
    public BooleanSetting khOpacity = new BooleanSetting("Хоткеи: Прозрачность", "").setValue(true);
    public SliderSettings khOpacityAmount = new SliderSettings("Хоткеи: Значение альфы", "").range(0.0f, 255.0f).setValue(150.0f);

    public BooleanSetting thBlur = new BooleanSetting("Таргет: Блюр", "").setValue(true);
    public SliderSettings thBlurAmount = new SliderSettings("Таргет: Сила блюра", "").range(1.0f, 15.0f).setValue(5.0f);
    public BooleanSetting thOpacity = new BooleanSetting("Таргет: Прозрачность", "").setValue(true);
    public SliderSettings thOpacityAmount = new SliderSettings("Таргет: Значение альфы", "").range(0.0f, 255.0f).setValue(150.0f);
    public ColorSetting targetHudColor = new ColorSetting("Target Hud: цвет фона", "Цвет фона таргет худа")
            .setColor(new Color(18, 19, 20, 200).getRGB());

    public BooleanSetting potionsBlur = new BooleanSetting("Potions: Блюр", "").setValue(false);
    public SliderSettings potionsBlurAmount = new SliderSettings("Potions: Сила блюра", "").range(1.0f, 15.0f).setValue(5.0f);
    public ColorSetting potionsColor = new ColorSetting("Potions: цвет фона", "Цвет фона блока зелий")
            .setColor(new Color(18, 19, 20, 250).getRGB());

    public ColorSetting colorSetting = new ColorSetting("Цвет клиента 1", "Первый цвет полосок (переливание)")
            .setColor(new Color(30, 30, 30, 255).getRGB()).presets(0xFF6C9AFD, 0xFF8C7FFF);
    public ColorSetting colorSetting2 = new ColorSetting("Цвет клиента 2", "Второй цвет полосок (переливание)")
            .setColor(new Color(120, 120, 120, 255).getRGB()).presets(0xFF8C7FFF, 0xFF6C9AFD);

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        setup(colorSetting, colorSetting2, interfaceSettings, notificationSettings, soundVolumeSetting,
                opacityWatermark, opacityHotKeys, opacityTargetHud, opacityArmorHud, opacityNotifications, opacityStaffList, opacityPotions,
                khBlur, khBlurAmount, khOpacity, khOpacityAmount,
                thBlur, thBlurAmount, thOpacity, thOpacityAmount, targetHudColor,
                potionsBlur, potionsBlurAmount, potionsColor);
    }
}