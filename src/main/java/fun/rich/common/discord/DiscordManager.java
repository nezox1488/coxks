package fun.rich.common.discord;
import antidaunleak.api.UserProfile;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Identifier;
import fun.rich.common.discord.utils.*;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.client.discord.Buffer;
import fun.rich.Rich;
import java.io.IOException;

@Setter
@Getter
public class DiscordManager implements QuickImports {
    private final DiscordDaemonThread discordDaemonThread = new DiscordDaemonThread();
    private boolean running = true;
    private DiscordInfo info = new DiscordInfo("Unknown", "", "");
    private Identifier avatarId;

    public void init() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return;
        }

        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .ready((user) -> {
                    Rich.getInstance().getDiscordManager().setInfo(
                            new DiscordInfo(user.username,
                                    "https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ".png",
                                    user.userId));
                    DiscordRichPresence richPresence = new DiscordRichPresence.Builder()
                            .setStartTimestamp(System.currentTimeMillis() / 1000)
                            .setDetails("User: " + UserProfile.getInstance().profile("username"))
                            .setState("Uid: " + UserProfile.getInstance().profile("uid"))
                            .setLargeImage("https://i.postimg.cc/d0WYPjGb/2026-02-15-15-32-52-online-video-cutter-com.gif", "https://t.me/roxsyclient")
                            .setSmallImage(Rich.getInstance().getDiscordManager().getInfo().avatarUrl, "https://roxsyclient.fun/")
                            .setButtons(RPCButton.create("Телеграм", "https://t.me/roxsyclient"),
                                    RPCButton.create("Дискорд", "discord.gg/soon..."))
                            .build();
                    DiscordRPC.INSTANCE.Discord_UpdatePresence(richPresence);
                }).build();
        DiscordRPC.INSTANCE.Discord_Initialize("1472575866629263455", handlers, true, "");
        discordDaemonThread.start();
    }

    public void stopRPC() {
        DiscordRPC.INSTANCE.Discord_Shutdown();
        this.running = false;
    }

    public void load() throws IOException {
        if (avatarId == null && !info.avatarUrl.isEmpty()) {
            avatarId = Buffer.registerDynamicTexture("avatar-", Buffer.getHeadFromURL(info.avatarUrl));
        }
    }

    public Identifier getAvatarId() {
        return avatarId;
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");
            try {
                while (Rich.getInstance().getDiscordManager().isRunning()) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    load();
                    Thread.sleep(15000);
                }
            } catch (Exception exception) {
                stopRPC();
            }
            super.run();
        }
    }

    public record DiscordInfo(String userName, String avatarUrl, String userId) {}
}