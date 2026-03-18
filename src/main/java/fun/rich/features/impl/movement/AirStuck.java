package fun.rich.features.impl.movement;


import fun.rich.events.packet.PacketEvent;
import fun.rich.events.player.MotionEvent;
import fun.rich.events.player.MoveEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.features.aura.warp.TurnsConnection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.Camera;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AirStuck extends Module {

    BooleanSetting grimBypass = new BooleanSetting("Обход грим", "Отправляет ground spoof пакет чтобы античит думал что мы стоим на земле");

    Vec3d frozenPosition = Vec3d.ZERO;
    int tickCounter = 0;

    public AirStuck() {
        super("AirStuck", "AirStuck", ModuleCategory.MOVEMENT);
        setup(grimBypass);
    }

    @Override
    public void activate() {
        super.activate();
        if (mc.player != null) {

            frozenPosition = mc.player.getPos();
            tickCounter = 0;
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        frozenPosition = Vec3d.ZERO;
        tickCounter = 0;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (frozenPosition == Vec3d.ZERO) return;


        mc.player.setPosition(frozenPosition);
        mc.player.prevX = frozenPosition.x;
        mc.player.prevY = frozenPosition.y;
        mc.player.prevZ = frozenPosition.z;
        mc.player.lastRenderX = frozenPosition.x;
        mc.player.lastRenderY = frozenPosition.y;
        mc.player.lastRenderZ = frozenPosition.z;


        mc.player.setVelocity(0, 0, 0);
        mc.player.velocityDirty = true;
        mc.player.fallDistance = 0;


        if (grimBypass.isValue()) {
            mc.player.setOnGround(true);
            mc.player.horizontalCollision = true;


            tickCounter++;
            if (tickCounter % 3 == 0) {

                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        frozenPosition.x,
                        frozenPosition.y + 1e-6,
                        frozenPosition.z,
                        mc.player.getYaw(),
                        mc.player.getPitch(),
                        true,
                        false
                ));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        frozenPosition.x,
                        frozenPosition.y + 1e-6,
                        frozenPosition.z,
                        mc.player.getYaw(),
                        mc.player.getPitch(),
                        true,
                        false
                ));

                mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
            }
        }
    }

    @EventHandler
    public void onMotion(MotionEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (frozenPosition == Vec3d.ZERO) return;


        e.setX(frozenPosition.x);
        e.setY(frozenPosition.y);
        e.setZ(frozenPosition.z);


        if (grimBypass.isValue()) {
            e.setOnGround(true);
        }
    }

    @EventHandler
    public void onMove(MoveEvent e) {
        if (mc.player == null || mc.world == null) return;


        e.setMovement(Vec3d.ZERO);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;

        if (e.getType() == PacketEvent.Type.SEND && e.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (frozenPosition != Vec3d.ZERO) {



                Camera camera = mc.gameRenderer.getCamera();
                float yaw = camera.getYaw();
                float pitch = camera.getPitch();


                e.cancel();



                boolean onGround = grimBypass.isValue();


                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        frozenPosition.x,
                        frozenPosition.y,
                        frozenPosition.z,
                        yaw,
                        pitch,
                        onGround,
                        false
                ));
            } else if (grimBypass.isValue()) {

                packet.onGround = true;
            }
        }
    }
}
