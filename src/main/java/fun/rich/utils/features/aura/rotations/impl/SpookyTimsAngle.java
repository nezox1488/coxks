package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Ротация SpookyTims v2: рандомные оффсеты, коррекция pitch при движении цели,
 * переменная скорость (быстрая до наведения, медленная при захвате), лимит 180°.
 */
public class SpookyTimsAngle extends RotateConstructor {

    private static final float LOOK_THRESHOLD = 12.0F;
    private static final float MAX_ROTATION_CAP = 180.0F;

    private final StopWatch lookLockWatch = new StopWatch();

    public SpookyTimsAngle() {
        super("SpookyTims");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        float targetYaw = targetAngle.getYaw();
        float targetPitch = targetAngle.getPitch();

        targetYaw += Calculate.getRandom(-5.0f, 5.0f);
        targetPitch += Calculate.getRandom(-5.0f, 5.0f);

        if (entity instanceof LivingEntity living && isMoving(living)) {
            double heightDiff = mc.player.getY() - living.getY() + 1.0;
            float sinOffset = (float) (5.0 + Math.sin((mc.player.age % 69) / 5.0 * 1512) * 35.0);
            float pitch = (float) (62.0 + heightDiff * Calculate.getRandom(-1.0f, sinOffset) - 5.0 + 10.0);
            targetPitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        }

        Turns modifiedTarget = new Turns(targetYaw, targetPitch);
        Turns delta = MathAngle.calculateDelta(currentAngle, modifiedTarget);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        float totalDiff = Math.abs(yawDelta) + Math.abs(pitchDelta);
        boolean isLookingAtTarget = totalDiff < LOOK_THRESHOLD;

        if (isLookingAtTarget) {
            // не сбрасываем таймер — цель в прицеле
        } else {
            lookLockWatch.reset();
        }

        float yawSpeed = Calculate.getRandom(60.0f, 80.0f);
        float pitchSpeed = Calculate.getRandom(40.0f, 80.0f);
        if (isLookingAtTarget && lookLockWatch.finished(100)) {
            yawSpeed = Calculate.getRandom(5.0f, 12.0f);
            pitchSpeed = Calculate.getRandom(5.0f, 12.0f);
        }

        float length = (float) Math.hypot(yawDelta, pitchDelta);
        if (length > 1.0E-3F) {
            float yawStep = Math.min(Math.abs(yawDelta), Math.min(yawSpeed, MAX_ROTATION_CAP)) * Math.signum(yawDelta);
            float pitchStep = Math.min(Math.abs(pitchDelta), Math.min(pitchSpeed, MAX_ROTATION_CAP)) * Math.signum(pitchDelta);
            float newYaw = currentAngle.getYaw() + yawStep;
            float newPitch = MathHelper.clamp(currentAngle.getPitch() + pitchStep, -89.0F, 90.0F);
            return new Turns(newYaw, newPitch).adjustSensitivity();
        }

        return modifiedTarget.adjustSensitivity();
    }

    private static boolean isMoving(LivingEntity target) {
        Vec3d v = target.getVelocity();
        return v.x != 0 || v.z != 0;
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }
}
