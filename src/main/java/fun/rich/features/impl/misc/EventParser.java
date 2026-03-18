package fun.rich.features.impl.misc;
/**
 * @author Sitoku
 * @since 3/3/2026
 */
import fun.rich.events.packet.PacketEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventParser extends Module {

    private final SliderSettings deleteDelay = new SliderSettings("Удалять через (мин)", "Через сколько удалить метку")
            .range(1, 20);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Pattern coordsPattern = Pattern.compile("(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)");
    private final Pattern colorPattern = Pattern.compile("(?i)§[0-9A-FK-OR]");

    public EventParser() {
        super("EventParser", "EventParser", ModuleCategory.MISC);
        setup(deleteDelay);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        String rawMessage = "";

        if (e.getPacket() instanceof ChatMessageS2CPacket packet) {
            if (packet.unsignedContent() != null) {
                rawMessage = packet.unsignedContent().getString();
            } else {
                rawMessage = packet.body().toString();
            }
        }
        else if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            rawMessage = packet.content().getString();
        }

        if (rawMessage == null || rawMessage.isEmpty()) return;

        String cleanMessage = colorPattern.matcher(rawMessage).replaceAll("");
        String lowerMsg = cleanMessage.toLowerCase();

        if (lowerMsg.contains("координаты") || lowerMsg.contains("статус") || lowerMsg.contains("появился на")) {
            parseEvent(cleanMessage);
        }
    }

    private void parseEvent(String msg) {
        String shortName = "";
        String lowerMsg = msg.toLowerCase();

        if (lowerMsg.contains("метеорит")) shortName = "Метеор";
        else if (lowerMsg.contains("маяк")) shortName = "Маяк";
        else if (lowerMsg.contains("мистич")) shortName = "Мистик";
        else if (lowerMsg.contains("вулкан")) shortName = "Вулкан";
        else if (lowerMsg.contains("дед мороз")) shortName = "Дед";
        else shortName = "Event";

        String cleanFromBrackets = msg.replace("[", " ").replace("]", " ").replace(",", " ");
        Matcher matcher = coordsPattern.matcher(cleanFromBrackets);

        if (matcher.find()) {
            String x = matcher.group(1);
            String y = matcher.group(2);
            String z = matcher.group(3);

            String addCommand = ".way add " + shortName + " " + x + " " + y + " " + z;

            if (mc.player != null) {

                mc.player.networkHandler.sendChatMessage(addCommand);

                mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
            }

            scheduleDeletion(shortName);
        }
    }

    private void scheduleDeletion(String shortName) {
        int delay = deleteDelay.getInt();
        scheduler.schedule(() -> {
            if (mc.player != null && this.isState()) {
                mc.player.networkHandler.sendChatMessage(".way remove " + shortName);
            }
        }, delay, TimeUnit.MINUTES);
    }
}