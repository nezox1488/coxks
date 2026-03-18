package fun.rich.display.screens.clickgui.components.implement.module;

import fun.rich.features.module.Module;
import fun.rich.utils.client.managers.localization.LocalizationManager;

public class ModuleDescriptions {
    private static final LocalizationManager loc = LocalizationManager.getInstance();

    public static String getDescription(Module module) {
        String moduleName = module.getClass().getSimpleName();
        String key = "desc." + moduleName;
        String translated = loc.translate(key);
        if (!translated.equals(key)) {
            return translated;
        }
        return loc.translate("desc.default");
    }
}