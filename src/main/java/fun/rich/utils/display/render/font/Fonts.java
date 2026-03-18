package fun.rich.utils.display.render.font;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import fun.rich.Rich;
import fun.rich.utils.client.logs.Logger;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Fonts {

    private static Font loadFontFromResource(String name) throws Exception {
        String otfPath = "assets/minecraft/fonts/" + name + ".otf";
        InputStream inputStream = Rich.class.getClassLoader().getResourceAsStream(otfPath);

        if (inputStream == null) {
            String ttfPath = "assets/minecraft/fonts/" + name + ".ttf";
            inputStream = Rich.class.getClassLoader().getResourceAsStream(ttfPath);
        }

        if (inputStream == null) {
            return null;
        }

        final InputStream finalInputStream = inputStream;
        try (finalInputStream) {
            return Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(finalInputStream));
        }
    }

    public static FontRenderer create(float size, String name) {
        try {
            Font font = loadFontFromResource(name);
            if (font != null) {
                font = font.deriveFont(Font.PLAIN, size / 2f);
                return new FontRenderer(font, size / 2f);
            }
        } catch (FontFormatException e) {
            Logger.error("Font file is corrupted or invalid: " + name + " (" + e.getMessage() + "), trying fallback", e);
        } catch (Exception e) {
            Logger.error("Failed to load font: " + name + " (" + e.getMessage() + "), trying fallback", e);
        }

        
        String[] fallbackNames = {"sf_regular", "sf_medium", "sf_bold", "sf_semibold"};
        for (String fallbackName : fallbackNames) {
            if (fallbackName.equals(name)) continue; 
            
            try {
                Font fallbackFont = loadFontFromResource(fallbackName);
                if (fallbackFont != null) {
                    Logger.warn("Using fallback font: " + fallbackName + " instead of " + name);
                    fallbackFont = fallbackFont.deriveFont(Font.PLAIN, size / 2f);
                    return new FontRenderer(fallbackFont, size / 2f);
                }
            } catch (Exception e) {
                
            }
        }

        
        Logger.warn("All fonts failed, using system font as last resort for: " + name);
        Font systemFont = new Font(Font.SANS_SERIF, Font.PLAIN, (int)(size / 2f));
        return new FontRenderer(systemFont, size / 2f);
    }

    private static final Map<FontKey, FontRenderer> fontCache = new HashMap<>();

    public static void init() {
        for (Type type : Type.values()) {
            for (int size = 4; size <= 32; size++) {
                try {
                    fontCache.put(new FontKey(size, type), create(size, type.getType()));
                } catch (Exception e) {
                    Logger.error("Failed to initialize font: " + type.getType() + " size " + size, e);
                    
                }
            }
        }
    }



    public static FontRenderer getSize(int size) {
        return getSize(size, Type.BOLD);
    }

    public static FontRenderer getSize(int size, Type type) {
        return fontCache.computeIfAbsent(new FontKey(size, type), k -> create(size, type.getType()));
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {
        DEFAULT("sf_medium"),
        REGULAR("sf_regular"),
        SEMI("sf_semibold"),
        BOLD("sf_bold"),
        ICONS("icons"),
        ICONS2("clienticon1"),
        ICONSTYPENEW("icon2"),
        GUIICONS("guiicons"),
        ESSENCE("essence"),
        SuisseIntlMedium("SuisseIntl-Medium"),
        SuisseIntlRegular("SuisseIntl-Regular"),
        SuisseIntlSemiBold("suisseintl-semibold"),
        ICONSCATEGORY("categoryicons"),
        WAYPOINT_ICONS("waypoint_icons"),;

        private final String type;
    }

    private record FontKey(int size, Type type) {
    }
}