package fun.rich.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.rich.main.listener.impl.EventListener;
import fun.rich.utils.client.chat.ChatMessage;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffects;
import fun.rich.features.module.setting.implement.MultiSelectSetting;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.client.Instance;
import fun.rich.events.player.TickEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoSprint extends Module {
    public static AutoSprint getInstance() {
        return Instance.get(AutoSprint.class);
    }

    public static int tickStop;

    MultiSelectSetting settings = new MultiSelectSetting("Отключать при эффекте", "Не дает спринтиться при эффектах")
            .value("Slowness", "Blindness");

    public AutoSprint() {
        super("AutoSprint", "Auto Sprint", ModuleCategory.MOVEMENT);
        setup(settings);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {

        boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
        boolean sneaking = mc.player.isSneaking() && !mc.player.isSwimming();
        if (!mc.player.isUsingItem()
                && !(settings.isSelected("Blindness") && mc.player.hasStatusEffect(StatusEffects.BLINDNESS))
                && !(settings.isSelected("Slowness") && mc.player.hasStatusEffect(StatusEffects.SLOWNESS))
                && tickStop > 0 || sneaking) {
            mc.player.setSprinting(false);
        } else if (!horizontal && mc.player.forwardSpeed > 0 && !mc.options.sprintKey.isPressed()) {
            mc.player.setSprinting(true);
        }

        tickStop--;
    }

}