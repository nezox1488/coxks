package fun.rich.features.impl.movement;

import fun.rich.events.player.InputEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.features.impl.combat.Aura;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.MultiSelectSetting;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.client.Instance;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.features.aura.warp.TurnsConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TargetStrafe extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public SelectSetting mode = new SelectSetting("Режим", "Тип стрейфа")
            .value("Matrix", "Grim")
            .selected("Matrix");

    SelectSetting type = new SelectSetting("Точка ходьбы", "Выбирете точку куда будет идти стрейф")
            .value("Cube", "Center")
            .selected("Cube").visible(() -> mode.isSelected("Grim"));

    SliderSettings grimRadius = new SliderSettings("Радиус куба", "Радиус обхода вокруг цели")
            .setValue(0.87F).range(0.1F, 1.5F).visible(() -> mode.isSelected("Grim"));

    MultiSelectSetting setting = new MultiSelectSetting("Настройки", "Позволяет настроить работу стрейфов")
            .value("Auto Jump", "Only Key Pressed", "In front of the target")
            .selected("Auto Jump");

    SliderSettings radius = new SliderSettings("Радиус", "Радиус обхода вокруг цели")
            .setValue(2.5F).range(0.1F, 7F).visible(() -> mode.isSelected("Matrix"));
    SliderSettings speed = new SliderSettings("Скорость", "Скорость стрейфа")
            .setValue(0.3F).range(0.1F, 1F).visible(() -> mode.isSelected("Matrix"));

    private int grimPointIndex = 0;

    public TargetStrafe() {
        super("TargetStrafe", "Target Strafe", ModuleCategory.MOVEMENT);
        setup(mode, type, grimRadius, radius, speed, setting);
    }

    public static TargetStrafe getInstance() {
        return Instance.get(TargetStrafe.class);
    }

    @EventHandler
    public void onInput(InputEvent event) {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null || !target.isAlive()) return;

        if (!mode.isSelected("Grim")) return;

        if (setting.isSelected("Only Key Pressed")) {
            if (!mc.options.forwardKey.isPressed() &&
                    !mc.options.backKey.isPressed() &&
                    !mc.options.leftKey.isPressed() &&
                    !mc.options.rightKey.isPressed()) {
                return;
            }
        }

        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        double r = grimRadius.getValue();

        Vec3d nextPoint;

        if (setting.isSelected("In front of the target")) {
            float targetYaw = target.getYaw();

            if (type.isSelected("Center")) {
                nextPoint = targetPos.add(-Math.sin(Math.toRadians(targetYaw)) * r, 0,
                        Math.cos(Math.toRadians(targetYaw)) * r);
            } else {
                double offset = Math.cos(System.currentTimeMillis() / 500.0) * 1;
                nextPoint = targetPos.add(
                        -Math.sin(Math.toRadians(targetYaw)) * r + Math.cos(Math.toRadians(targetYaw)) * offset,
                        0,
                        Math.cos(Math.toRadians(targetYaw)) * r + Math.sin(Math.toRadians(targetYaw)) * offset
                );
            }

        } else {
            if (type.isSelected("Cube")) {
                Vec3d[] points = new Vec3d[]{
                        new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                        new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                        new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                        new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
                };

                nextPoint = points[grimPointIndex];
                if (playerPos.distanceTo(nextPoint) < 0.5) {
                    grimPointIndex = (grimPointIndex + 1) % points.length;
                }
            } else {
                nextPoint = new Vec3d(targetPos.x, playerPos.y, targetPos.z);
            }
        }

        Vec3d direction = nextPoint.subtract(playerPos).normalize();

        float yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
        float movementAngle = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F;
        float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);

        boolean forward = false, back = false, left = false, right = false;

        if (angleDiff >= -22.5 && angleDiff < 22.5) {
            forward = true;
        } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
            forward = true; right = true;
        } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
            right = true;
        } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
            back = true; right = true;
        } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
            forward = true; left = true;
        } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
            left = true;
        } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
            back = true; left = true;
        } else {
            back = true;
        }

        event.setDirectional(forward, back, left, right);

        if (setting.isSelected("Auto Jump") && mc.player.isOnGround()) {
            event.setJumping(true);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null || !target.isAlive()) return;

        if (!mode.isSelected("Matrix")) return;

        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        double r = radius.getValue();

        if (setting.isSelected("Only Key Pressed")) {
            if (!mc.options.forwardKey.isPressed() &&
                    !mc.options.backKey.isPressed() &&
                    !mc.options.leftKey.isPressed() &&
                    !mc.options.rightKey.isPressed()) {
                return;
            }
        }

        if (setting.isSelected("Auto Jump") && mc.player.isOnGround()) {
            mc.player.jump();
        }

        if (setting.isSelected("In front of the target")) {
            float targetYaw = target.getYaw();
            double x = targetPos.x - Math.sin(Math.toRadians(targetYaw)) * r;
            double z = targetPos.z + Math.cos(Math.toRadians(targetYaw)) * r;

            float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90F;
            double motionSpeed = speed.getValue();
            mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
            return;
        }

        double angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
        angle += speed.getValue() / Math.max(playerPos.distanceTo(targetPos), r);

        double x = targetPos.x + r * Math.cos(angle);
        double z = targetPos.z + r * Math.sin(angle);

        float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90F;
        double motionSpeed = speed.getValue();
        mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed,
                mc.player.getVelocity().y,
                Math.cos(Math.toRadians(yaw)) * motionSpeed);
    }
    @Override
    public void activate() {
        super.activate();
        grimPointIndex = 0;
    }
}
