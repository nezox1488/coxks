package fun.rich.features.module;

import lombok.Getter;

@Getter
public enum ModuleCategory {

    COMBAT("Combat", -3),
    MOVEMENT("Movement", 0),
    RENDER("Render", 0),
    PLAYER("Player", 0),
    MISC("Misc", 0),
    CONFIGS("Configs", 0),
    AUTOBUY("AutoBuy", -2),
    THEME("Theme", 0);

    final String readableName;
    final float iconOffsetY;

    ModuleCategory(String readableName, float iconOffsetY) {
        this.readableName = readableName;
        this.iconOffsetY = iconOffsetY;
    }
}