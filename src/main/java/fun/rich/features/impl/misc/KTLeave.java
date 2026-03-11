 package fun.rich.features.impl.misc;

import fun.rich.events.packet.PacketEvent;
import fun.rich.events.render.WorldRenderEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.geometry.Render3D;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class KTLeave extends Module {

    BooleanSetting drawGhost = new BooleanSetting("Рисовать фантома", "Показывает место клика").setValue(true);
    BooleanSetting retry = new BooleanSetting("Перезапустить заморозку", "Позволяет поймать пакет еще раз").setValue(false);
    BooleanSetting chatTeleport = new BooleanSetting("Телепорт по чату", "Ловить клик по [Телепортироваться досрочно]").setValue(true);

    final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    Box ghostBox;
    boolean captured = false;
    boolean waitingTeleportClick = false;

    public KTLeave() {
        super("KTLeave", "KTLeave", ModuleCategory.MISC);
        setup(drawGhost, retry, chatTeleport);
    }

    @Override
    public void activate() {
        resetCapture();
        super.activate();
    }

    @Override
    public void deactivate() {
        if (!packets.isEmpty()) {
            packets.forEach(p -> mc.player.networkHandler.sendPacket(p));
            packets.clear();
        }
        resetCapture();
        super.deactivate();
    }

    private void resetCapture() {
        captured = false;
        waitingTeleportClick = false;
        packets.clear();
        ghostBox = null;
        retry.setValue(false);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;

        if (retry.isValue()) {
            resetCapture();
            mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
            return;
        }

        if (captured) return;

        Packet<?> packet = e.getPacket();

        if (e.isSend()) {
            // Захватываем команду, которая отправляется при нажатии
            // на кнопку [Телепортироваться досрочно] в чате
            if (chatTeleport.isValue() && waitingTeleportClick && packet instanceof ChatMessageC2SPacket) {
                capture(packet);
                waitingTeleportClick = false;
                e.cancel();
                return;
            }

            if (packet instanceof PlayerInteractEntityC2SPacket interact) {
                if (!interact.toString().toLowerCase().contains("attack")) {
                    capture(packet);
                    e.cancel();
                }
            } else if (packet instanceof PlayerInteractBlockC2SPacket) {
                capture(packet);
                e.cancel();
            }
        }
    }

    private void capture(Packet<?> packet) {
        packets.add(packet);
        captured = true;
        ghostBox = mc.player.getBoundingBox();
        mc.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (drawGhost.isValue() && ghostBox != null && captured && this.isState()) {
            int color = ColorAssist.getColor(255, 215, 0);
            Render3D.drawBox(ghostBox, color, 1.0f, true, true, true);
        }
    }

    /**
     * Вызывается из ChatHudMixin, когда в чате появляется
     * сообщение с кнопкой [Телепортироваться досрочно].
     */
    public void notifyTeleportPrompt() {
        if (!this.isState()) return;
        waitingTeleportClick = true;
    }
}

