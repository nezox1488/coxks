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
import fun.rich.utils.features.aura.warp.TurnsConfig;
import fun.rich.utils.features.aura.warp.TurnsConnection;

import java.security.SecureRandom;

public class SPAngle extends RotateConstructor {
    private static final float ROTATION_SPEED = 25.5F;
    private static final float LIMIT_ROTATION_SPEED = 44.5F;
    private static final float SHAKE_INTENSITY = 2.2F;
    private static final float SHAKE_SPEED = 0.32F;
    private static final float EPSILON = 1.0E-3F;
    private static final SecureRandom RANDOM = new SecureRandom();
    public SPAngle() {
        super("SpookyTime");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Aura aura = Aura.getInstance();

        if (aura != null && aura.neuroExec()) {
            StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
            var controller = TurnsConnection.INSTANCE;
            TurnsConfig rotationConfig = aura.getRotationConfig();

            var config = aura.getConfig();

            boolean applied = aura.getNeuroExecEngine()
                    .tryApplyExecRotation(aura, config, attackHandler, controller, rotationConfig);
            if (applied) {
                return currentAngle.adjustSensitivity();
            }
        }

        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();

        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float length = (float) Math.hypot(yawDelta, pitchDelta);
        final float ANGLE_LIMIT_YAW = (float) Math.min(Math.abs(yawDelta), 74.0F);
        final float ANGLE_LIMIT_PITCH = (float) Math.min(Math.abs(pitchDelta), 32.334F);
        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());

        if (length > EPSILON) {
            boolean limitReached = Math.abs(pitchDelta) >= ANGLE_LIMIT_PITCH;
            float maxStep = limitReached ? LIMIT_ROTATION_SPEED : ROTATION_SPEED;
            float step = Math.min(length, maxStep);
            float scale = step / length;
            if (!limitReached) {
                scale = easeTowardsTarget(scale);
            }

            float newPitch = MathHelper.clamp(currentAngle.getPitch() + pitchDelta * scale, -89.0F, 90.0F);
            moveAngle.setPitch(newPitch);
        }

        if (length > EPSILON) {
            boolean limitReached = Math.abs(yawDelta) >= ANGLE_LIMIT_YAW;
            float maxStep = limitReached ? LIMIT_ROTATION_SPEED : ROTATION_SPEED;
            float step = Math.min(length, maxStep);
            float scale = step / length;
            if (!limitReached) {
                scale = easeTowardsTarget(scale);
            }

            float newYaw = currentAngle.getYaw() + yawDelta * scale;
            moveAngle.setYaw(newYaw);
        }

        return moveAngle.adjustSensitivity();
    }

    private void applyBodyMotion(Turns angle) {
        if (mc.player == null) return;

        float time = (System.currentTimeMillis() % 12000L) / 1200.0F;
        float swayPhase = time * SHAKE_SPEED * MathHelper.TAU;
        float swayYaw = MathHelper.sin(swayPhase) * SHAKE_INTENSITY;

        angle.setYaw(angle.getYaw() + swayYaw);
    }

    private float easeTowardsTarget(float value) {
        return value * (0.5F + 0.5F * value);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }
}