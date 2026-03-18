package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.striking.StrikeManager;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fun.rich.Rich;

import java.security.SecureRandom;

public class FuntimeTestAngle extends RotateConstructor {

    public FuntimeTestAngle() {
        super("FuntimeTest");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        int count = attackHandler.getCount();
        StopWatch attackTimer = attackHandler.getAttackTimer();

        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (entity != null) {
            float speed = attackHandler.canAttack(Aura.getInstance().getConfig(), 0)
                    ? 1
                    : new SecureRandom().nextBoolean() ? 0.4F : 0.2F;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float lerpFactorYaw = rand(speed, speed + 0.2F);
            float lerpFactorPitch = rand(speed, speed + 0.2F);

            return new Turns(
                    MathHelper.lerp(lerpFactorYaw, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw),
                    MathHelper.lerp(lerpFactorPitch, currentAngle.getPitch(), currentAngle.getPitch() + movePitch)
            );
        } else {
            Turns lerp = switch (count % 3) {
                case 0 -> new Turns(
                        (float) Math.cos(attackTimer.elapsedTime() / 40F + (count % 6)),
                        (float) Math.sin(attackTimer.elapsedTime() / 40F + (count % 6))
                );
                case 1 -> new Turns(
                        (float) Math.sin(attackTimer.elapsedTime() / 40F + (count % 6)),
                        (float) Math.cos(attackTimer.elapsedTime() / 40F + (count % 6))
                );
                case 2 -> new Turns(
                        (float) Math.sin(attackTimer.elapsedTime() / 40F + (count % 6)),
                        (float) -Math.cos(attackTimer.elapsedTime() / 40F + (count % 6))
                );
                default -> new Turns(
                        (float) -Math.cos(attackTimer.elapsedTime() / 40F + (count % 6)),
                        (float) Math.sin(attackTimer.elapsedTime() / 40F + (count % 6))
                );
            };

            float yaw = !attackTimer.finished(2000)
                    ? rand(12, 24) * lerp.getYaw()
                    : 0;
            float pitch2 = rand(0, 2) * (float) Math.cos((double) System.currentTimeMillis() / 5000);
            float pitch = !attackTimer.finished(2000)
                    ? rand(2, 6) * lerp.getPitch() + pitch2
                    : 0;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float baseSpeed = attackTimer.finished(400)
                    ? new SecureRandom().nextBoolean() ? 0.4F : 0.2F
                    : -0.2F;

            float lerpFactorYaw = (float) Math.clamp(rand(baseSpeed, baseSpeed + 0.2F), 0, 1);
            float lerpFactorPitch = (float) Math.clamp(rand(baseSpeed, baseSpeed + 0.2F), 0, 1);

            return new Turns(
                    MathHelper.lerp(lerpFactorYaw, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + yaw,
                    MathHelper.lerp(lerpFactorPitch, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + pitch
            );
        }
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.05, 0.1, 0.02);
    }

    private float rand(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }
}

