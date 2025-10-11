package fun.rich.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.rich.events.player.TickEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.math.time.TimerUtil;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GrimGlide extends Module {
    TimerUtil ticks = new TimerUtil();

    public GrimGlide() {
        super("Elytra Glide", "Elytra Glide", ModuleCategory.MOVEMENT);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isGliding()) return;

        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double forward = 0.087;
        double motion = calculateBPS();
        float valuePidor = 52;

        if (motion >= valuePidor) {
            forward = 0f;
        }

        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;

        mc.player.setVelocity(
                dx * Calculate.getRandom(1.1f, 1.21f),
                mc.player.getVelocity().y - 0.02f,
                dz * Calculate.getRandom(1.1f, 1.21f)
        );

        if (ticks.hasTimeElapsed(50)) {
            mc.player.setPosition(
                    pos.getX() + dx,
                    pos.getY(),
                    pos.getZ() + dz
            );
            ticks.resetCounter();
        }

        mc.player.setVelocity(
                dx * Calculate.getRandom(1.1f, 1.21f),
                mc.player.getVelocity().y + 0.016f,
                dz * Calculate.getRandom(1.1f, 1.21f)
        );
    }

    private double calculateBPS() {
        if (mc.player == null) return 0;
        Vec3d velocity = mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20;
    }
}