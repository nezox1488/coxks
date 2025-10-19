package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.utils.features.aura.point.Vector;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.math.calc.Calculate;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fun.rich.features.impl.combat.Aura;
import fun.rich.Rich;
import fun.rich.utils.features.aura.striking.StrikeManager;
import fun.rich.utils.features.aura.utils.MathAngle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import fun.rich.utils.math.time.StopWatch;

import java.security.SecureRandom;

public class SPAngle extends RotateConstructor {
    private boolean redirectDirectionChosen = false;
    private boolean redirectToRight = true;
    public SPAngle() {
        super("SpookyTime");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        Aura aura = Aura.getInstance();
        int count = attackHandler.getCount();

        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        float speed = canAttack ? 0.8f : 0.67F;
        float jitterYaw = canAttack ? 0 : Calculate.getRandom(-4, 4);
        float jitterPitch = canAttack ? 0 : Calculate.getRandom(-3, 3);

        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 100);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
        if (entity instanceof LivingEntity livingEntity) {
            double targetHeight = livingEntity.getHeight();
            double torsoHeight = targetHeight * 0.36;
            Vec3d playerPos = MinecraftClient.getInstance().player.getPos().add(0, 1.5, 0);
            Vec3d entityPos = livingEntity.getPos();
            double deltaY = (entityPos.y + torsoHeight) - playerPos.y;
            double deltaX = entityPos.x - playerPos.x;
            double deltaZ = entityPos.z - playerPos.z;
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float torsoPitch = (float) Math.toDegrees(-Math.atan2(deltaY, horizontalDistance));
            torsoPitch = MathHelper.clamp(torsoPitch, -90.0f, 90.0f);

            if (currentAngle.getPitch() > torsoPitch) {
                float pitchAdjustment = Math.min(8.0f, currentAngle.getPitch() - torsoPitch);
                movePitch -= pitchAdjustment;
            }
        }
        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);

        float pitchSpeed = pitchDelta < 0 ? 0.45F : 0.8F;
        moveAngle.setPitch(MathHelper.lerp(pitchSpeed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);
        return new Turns(moveAngle.getYaw(), moveAngle.getPitch());
    }


    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}