package fun.rich.utils.client.managers.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fun.rich.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import fun.rich.display.screens.clickgui.components.implement.autobuy.originalitems.ItemRegistry;
import fun.rich.utils.client.managers.file.ClientFile;
import fun.rich.utils.client.managers.file.exception.FileLoadException;
import fun.rich.utils.client.managers.file.exception.FileSaveException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AutoBuyConfigFile extends ClientFile {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AutoBuyConfigFile() {
        super("autobuy");
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        File file = new File(path, getName() + ".json");
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (AutoBuyableItem item : ItemRegistry.getAllItems()) {
                    if (json.has(item.getDisplayName())) {
                        item.setEnabled(json.get(item.getDisplayName()).getAsBoolean());
                    }
                }
            }
        } catch (IOException e) {
            throw new FileLoadException("Failed to load " + getName() + " from file", e);
        }
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        JsonObject json = new JsonObject();
        for (AutoBuyableItem item : ItemRegistry.getAllItems()) {
            json.addProperty(item.getDisplayName(), item.isEnabled());
        }
        File file = new File(path, getName() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            throw new FileSaveException("Failed to save " + getName() + " to file", e);
        }
    }
}