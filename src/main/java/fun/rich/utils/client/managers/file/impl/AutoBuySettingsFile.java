package fun.rich.utils.client.managers.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fun.rich.display.screens.clickgui.components.implement.autobuy.settings.AutoBuySettingsManager;
import fun.rich.display.screens.clickgui.components.implement.autobuy.originalitems.ItemRegistry;
import fun.rich.utils.client.managers.file.ClientFile;
import fun.rich.utils.client.managers.file.exception.FileLoadException;
import fun.rich.utils.client.managers.file.exception.FileSaveException;
import java.io.File;

public class AutoBuySettingsFile extends ClientFile {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AutoBuySettingsFile() {
        super("autobuy_settings");
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        File file = new File(path, getName() + ".json");
        AutoBuySettingsManager.getInstance().loadFromFile(file);
        ItemRegistry.reloadSettings();
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        File file = new File(path, getName() + ".json");
        AutoBuySettingsManager.getInstance().saveToFile(file);
    }
}