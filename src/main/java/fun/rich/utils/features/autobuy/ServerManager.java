package fun.rich.utils.features.autobuy;

import java.util.ArrayList;
import java.util.List;

public class ServerManager {
    private final List<String> anarchyServers165;
    private final List<String> anarchyServers214;
    private int currentIndex165 = 0;
    private int currentIndex214 = 0;
    private String currentServer = "";

    public ServerManager() {
        anarchyServers165 = new ArrayList<>();
        anarchyServers214 = new ArrayList<>();

        anarchyServers165.addAll(List.of("/an102", "/an103", "/an104", "/an105", "/an106", "/an107"));
        for (int i = 203; i <= 221; i++) {
            anarchyServers165.add("/an" + i);
        }
        for (int i = 302; i <= 313; i++) {
            anarchyServers165.add("/an" + i);
        }
        anarchyServers165.addAll(List.of("/an502", "/an503", "/an504", "/an505", "/an506", "/an507", "/an602"));

        anarchyServers214.addAll(List.of("/an11", "/an12", "/an21", "/an23", "/an31", "/an32", "/an51", "/an52"));
    }

    public String getNextServer(boolean is1214) {
        if (is1214) {
            if (currentIndex214 >= anarchyServers214.size()) {
                currentIndex214 = 0;
            }
            currentServer = anarchyServers214.get(currentIndex214);
            currentIndex214++;
            return currentServer;
        } else {
            if (currentIndex165 >= anarchyServers165.size()) {
                currentIndex165 = 0;
            }
            currentServer = anarchyServers165.get(currentIndex165);
            currentIndex165++;
            return currentServer;
        }
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public void setCurrentServer(String server) {
        this.currentServer = server;
    }

    public void reset() {
        currentIndex165 = 0;
        currentIndex214 = 0;
        currentServer = "";
    }
}