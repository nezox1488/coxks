package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.Rich;
import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.features.aura.rotations.MXBypassUtils;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.striking.StrikeManager;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;


public class MXAngle extends RotateConstructor {
    private final SecureRandom random = new SecureRandom();

    public MXAngle() {
        super("MX");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        boolean canAttack = entity != null && attackHandler.canAttack(Aura.getInstance().getConfig(), 0);

        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float rotationDiff = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDiff < 1e-6f) {
            return new Turns(currentAngle.getYaw(), MXBypassUtils.sanitizePitch(currentAngle.getPitch()));
        }

        float speed = canAttack ? randomLerp(0.5f, 0.7f) : randomLerp(0.25f, 0.4f);
        if (!attackTimer.finished(300)) speed = 0.15f;
        if (!attackTimer.finished(500)) speed = 0.3f;

        float lineYaw = Math.abs(yawDelta / rotationDiff) * 180;
        float linePitch = Math.abs(pitchDelta / rotationDiff) * 180;
        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float newYaw = MathHelper.lerp(speed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw);
        float newPitch = MathHelper.lerp(speed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch);

        newYaw += MXBypassUtils.deltaNoise();
        newPitch = MXBypassUtils.sanitizePitch(newPitch);

        return new Turns(newYaw, MathHelper.clamp(newPitch, -89.99f, 89.99f));
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}
