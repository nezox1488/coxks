package fun.rich.common.proxy;

import antidaunleak.api.annotation.Native;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.gui.widget.ButtonWidget;

public class ProxyServer implements ModInitializer {
    public static boolean proxyEnabled = false;
    public static Proxy proxy = new Proxy();
    public static Proxy lastUsedProxy = new Proxy();
    public static ButtonWidget proxyMenuButton;

    public static String getLastUsedProxyIp() {
        return lastUsedProxy.ipPort.isEmpty() ? "none" : lastUsedProxy.getIp();
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onInitialize() {
        Config.loadConfig();
    }
}