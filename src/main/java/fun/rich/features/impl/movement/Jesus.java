package fun.rich.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.rich.events.packet.PacketEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.interactions.simulate.Simulations;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Jesus extends Module {

    private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения по воде")
            .value("Matrix")
            .selected("Matrix");
    private final StopWatch timer = new StopWatch();
    @NonFinal
    private boolean isMoving;

    public Jesus() {
        super("Jesus", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @EventHandler
    public void tick(TickEvent update) {
        if (mc.player.isTouchingWater() && !mc.player.isSubmergedInWater() && mode.isSelected("Matrix")) {
            Vec3d vel = mc.player.getVelocity();
            if (isMoving) {
                double boost = 1;
                mc.player.setVelocity(
                        vel.x * boost,
                        Math.max(0, vel.y),
                        vel.z * boost
                );
            } else {
                mc.player.setVelocity(
                        vel.x * 0.2,
                        Math.max(0, vel.y),
                        vel.z * 0.2
                );
            }

            if (mc.player.getVelocity().y < 0) {
                mc.player.setVelocity(
                        mc.player.getVelocity().x,
                        0.05,
                        mc.player.getVelocity().z
                );
            }
        }
    }

}