package fun.rich.features.impl.misc;

import fun.rich.Rich;
import fun.rich.events.chat.ChatEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.client.managers.file.FileRepository;
import fun.rich.utils.client.managers.file.exception.FileLoadException;
import fun.rich.utils.client.managers.file.impl.PrefixFile;
import fun.rich.utils.math.calc.Calculate;
import org.apache.logging.log4j.core.appender.rolling.action.IfAll;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MathUtil;

public class SelfDestruct extends Module {
    public static boolean unhooked;

    public SelfDestruct() {
        super("SelfDestruct", "Self Destruct", ModuleCategory.MISC);
    }

    @Override
    public void activate() {
        unhooked = true;

        for (Module module : Rich.getInstance().getModuleProvider().getModules()) {
            if (module != this && module.isState()) {
                module.setState(false);
            }
        }

        for (Module module : Rich.getInstance().getModuleProvider().getModules()) {
            module.setKey(GLFW.GLFW_KEY_UNKNOWN);
        }

        Rich.getInstance().getCommandDispatcher().prefix = "" + Calculate.getRandom(0, 9999999);

        super.activate();
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("UNHOOK=FALSE")) {
            unhooked = false;
            state = false;
            Rich.getInstance().getCommandDispatcher().prefix = ".";
            ChatMessage.brandmessage("Unhook reset to FALSE");
            event.setCancelled(true);
        }
    }

    @Override
    public void deactivate() {
        unhooked = false;
        super.deactivate();
    }
}
