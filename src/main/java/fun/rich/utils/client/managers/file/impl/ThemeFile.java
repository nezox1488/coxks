package fun.rich.utils.client.managers.file.impl;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.rich.display.screens.clickgui.components.implement.themes.ThemeManager;
import fun.rich.features.module.setting.implement.ColorSetting;
import fun.rich.utils.client.managers.file.ClientFile;
import fun.rich.utils.client.managers.file.exception.FileLoadException;
import fun.rich.utils.client.managers.file.exception.FileSaveException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ThemeFile extends ClientFile {
    Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    ThemeManager themeManager;

    public ThemeFile(ThemeManager themeManager) {
        super("theme");
        this.themeManager = themeManager;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        saveToFile(path, getName() + ".json");
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        loadFromFile(path, getName() + ".json");
    }

    @Override
    public void saveToFile(File path, String fileName) throws FileSaveException {
        JsonObject themeObject = createJsonObjectFromTheme();
        File file = new File(path, fileName);
        writeJsonToFile(themeObject, file);
        super.saveToFile(path, fileName);
    }

    @Override
    public void loadFromFile(File path, String fileName) throws FileLoadException {
        File file = new File(path, fileName);
        JsonObject themeObject = readJsonFromFile(file);
        if (themeObject != null) {
            updateThemeFromJsonObject(themeObject);
        }
        super.loadFromFile(path, fileName);
    }

    private JsonObject createJsonObjectFromTheme() {
        JsonObject themeObject = new JsonObject();
        JsonObject interfaceObject = new JsonObject();
        JsonObject clickGuiObject = new JsonObject();

        for (ColorSetting color : themeManager.getInterfaceColors()) {
            interfaceObject.addProperty(color.getName(), color.getColor());
        }

        for (ColorSetting color : themeManager.getClickGuiColors()) {
            clickGuiObject.addProperty(color.getName(), color.getColor());
        }

        themeObject.add("interface", interfaceObject);
        themeObject.add("clickgui", clickGuiObject);

        return themeObject;
    }

    private void updateThemeFromJsonObject(JsonObject themeObject) {
        if (themeObject.has("interface")) {
            JsonObject interfaceObject = themeObject.getAsJsonObject("interface");
            for (ColorSetting color : themeManager.getInterfaceColors()) {
                if (interfaceObject.has(color.getName())) {
                    color.setColor(interfaceObject.get(color.getName()).getAsInt());
                }
            }
        }

        if (themeObject.has("clickgui")) {
            JsonObject clickGuiObject = themeObject.getAsJsonObject("clickgui");
            for (ColorSetting color : themeManager.getClickGuiColors()) {
                if (clickGuiObject.has(color.getName())) {
                    color.setColor(clickGuiObject.get(color.getName()).getAsInt());
                }
            }
        }
    }

    private void writeJsonToFile(JsonObject themeObject, File file) throws FileSaveException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(themeObject, writer);
        } catch (IOException e) {
            throw new FileSaveException("Failed to save theme to file", e);
        }
    }

    private JsonObject readJsonFromFile(File file) throws FileLoadException {
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new FileLoadException("Failed to load theme from file", e);
        } catch (JsonSyntaxException | JsonIOException e) {
            throw new FileLoadException("Failed to parse JSON from file", e);
        }
    }
}