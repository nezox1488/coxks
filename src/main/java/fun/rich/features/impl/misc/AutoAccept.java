package fun.rich.features.impl.misc;

import fun.rich.common.repository.friend.FriendUtils;
import fun.rich.events.packet.PacketEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AutoAccept extends Module {

    final String[] teleportMessages = {
            "has requested teleport", "просит телепортироваться",
            "хочет телепортироваться", "просит к вам телепортироваться"
    };

    private final Pattern regionPattern = Pattern.compile("регион.*?создан.*?#(\\d+)");
    private static final Pattern INVITE_RU = Pattern.compile(
            "(?:^|\\s)([A-Za-z0-9_]{3,16})\\s+приглашает\\s+вас\\s+в\\s+клан",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INVITE_RU_LOOSE = Pattern.compile(
            "(?:^|\\s)([A-Za-z0-9_]{3,16})\\s+приглашает.*?клан",
            Pattern.CASE_INSENSITIVE
    );

    final BooleanSetting autoTp = new BooleanSetting("Авто-ТП", "Принимать запросы на телепортацию").setValue(true);
    final BooleanSetting onlyFriends = new BooleanSetting("Только друзья", "Принимать всё только от друзей").setValue(true);
    final BooleanSetting autoRegion = new BooleanSetting("Авто-регион", "Добавлять друзей в новый РГ").setValue(true);
    final BooleanSetting autoClan = new BooleanSetting("Авто-клан", "Принимать инвайты в клан").setValue(true);

    private long lastClanAcceptTime = 0;

    public AutoAccept() {
        super("AutoAccept", "AutoAccept", ModuleCategory.MISC);
        setup(autoTp, onlyFriends, autoRegion, autoClan);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!(e.getPacket() instanceof GameMessageS2CPacket packet)) return;

        String rawMsg = packet.content().getString();
        if (rawMsg == null || rawMsg.isEmpty()) return;

        String msg = rawMsg.replaceAll("§.", "");
        String lowerMsg = msg.toLowerCase();

        if (autoTp.isValue() && Arrays.stream(teleportMessages).anyMatch(lowerMsg::contains)) {
            if (checkFriend(lowerMsg)) {
                mc.player.networkHandler.sendChatCommand("tpaccept");
            }
        }

        if (autoClan.isValue() && lowerMsg.contains("приглашает") && lowerMsg.contains("клан")) {
            String inviter = extractInviter(msg);
            if (inviter != null && checkFriend(inviter.toLowerCase())) {
                long now = System.currentTimeMillis();
                if (now - lastClanAcceptTime > 800) {
                    mc.player.networkHandler.sendChatCommand("clan accept " + inviter);
                    lastClanAcceptTime = now;
                }
            }
        }

        if (autoRegion.isValue() && lowerMsg.contains("регион") && lowerMsg.contains("создан")) {
            Matcher matcher = regionPattern.matcher(lowerMsg);
            if (matcher.find()) {
                String regionId = matcher.group(1);
                addFriendsToRegion(regionId);
            }
        }
    }

    private String extractInviter(String msg) {
        Matcher m = INVITE_RU.matcher(msg);
        if (m.find()) return m.group(1);

        m = INVITE_RU_LOOSE.matcher(msg);
        if (m.find()) return m.group(1);

        return null;
    }

    private void addFriendsToRegion(String id) {
        if (mc.getNetworkHandler() == null) return;

        List<String> onlinePlayers = mc.getNetworkHandler().getPlayerList().stream()
                .map(entry -> entry.getProfile().getName().toLowerCase())
                .collect(Collectors.toList());

        FriendUtils.getFriends().forEach(friend -> {
            String name = friend.getName();
            if (onlinePlayers.contains(name.toLowerCase())) {
                mc.player.networkHandler.sendChatCommand("ps add " + name + " " + id);
            }
        });
    }

    private boolean checkFriend(String text) {
        if (!onlyFriends.isValue()) return true;

        return FriendUtils.getFriends().stream()
                .anyMatch(f -> text.contains(f.getName().toLowerCase()));
    }
}