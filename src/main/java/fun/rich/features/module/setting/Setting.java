package fun.rich.features.module.setting;

import fun.rich.utils.client.managers.localization.LocalizationManager;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
public class Setting {
    private final String name;
    private String description;

    @Setter
    private Supplier<Boolean> visible;

    private static final LocalizationManager loc = LocalizationManager.getInstance();

    public Setting(String name) {
        this.name = name;
    }

    public Setting(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public boolean isVisible() {
        return visible == null || visible.get();
    }

    public String getLocalizedName() {
        String key = "setting." + name;
        String translated = loc.translate(key);
        return translated.equals(key) ? name : translated;
    }

    public String getLocalizedDescription() {
        if (description == null) return null;
        String key = "setting.desc." + name;
        String translated = loc.translate(key);
        return translated.equals(key) ? description : translated;
    }
}
