package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.striking.StrikeManager;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.math.time.StopWatch;
import fun.rich.Rich;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

/**
 * Ротация для булавы: при полёте вниз на игрока идеально попадает при приземлении.
 * Основана на FTAngle, но при падении (velocity.y < 0) использует более агрессивную
 * и прямую наводку для точного попадания при касании земли.
 */
public class FTBulava extends RotateConstructor {

    public FTBulava() {
        super("FTBulava");
    }

    @Override
    public Turns limitAngleChange(Turns currentTurns, Turns targetTurns, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        int count = attackHandler.getCount();

        Turns turnsDelta = MathAngle.calculateDelta(currentTurns, targetTurns);
        float yawDelta = turnsDelta.getYaw();
        float pitchDelta = turnsDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotationDifference < 1e-6f) {
            return new Turns(currentTurns.getYaw(), currentTurns.getPitch());
        }

        boolean isFalling = mc.player != null && mc.player.getVelocity().y < -0.08;

        if (entity != null) {
            // При падении — быстрая прямая наводка для попадания при приземлении
            float speed = isFalling ? randomLerp(0.85f, 1.0f) : (attackHandler.canAttack(Aura.getInstance().getConfig(), 0) ? 0.7f : randomLerp(0.35f, 0.5f));

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            Turns moveTurns = new Turns(currentTurns.getYaw(), currentTurns.getPitch());
            moveTurns.setYaw(MathHelper.lerp(randomLerp(speed, Math.min(speed + 0.2f, 1f)), currentTurns.getYaw(), currentTurns.getYaw() + moveYaw));
            moveTurns.setPitch(MathHelper.lerp(randomLerp(speed, Math.min(speed + 0.2f, 1f)), currentTurns.getPitch(), currentTurns.getPitch() + movePitch));

            return moveTurns;
        } else {
            int suck = count % 3;
            float speed = attackTimer.finished(430) ? (new SecureRandom().nextBoolean() ? 0.4f : 0.2f) : -0.2f;
            float random = attackTimer.elapsedTime() / 40f + (count % 6);

            Turns randomTurns = switch (suck) {
                case 0 -> new Turns((float) Math.cos(random), (float) Math.sin(random));
                case 1 -> new Turns((float) Math.sin(random), (float) Math.cos(random));
                case 2 -> new Turns((float) Math.sin(random), (float) -Math.cos(random));
                default -> new Turns((float) -Math.cos(random), (float) Math.sin(random));
            };

            float yaw = !attackTimer.finished(2000) ? randomLerp(12, 24) * randomTurns.getYaw() : 0;
            float pitch2 = randomLerp(0, 2) * (float) Math.cos((double) System.currentTimeMillis() / 5000);
            float pitch = !attackTimer.finished(2000) ? randomLerp(2, 6) * randomTurns.getPitch() + pitch2 : 0;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            Turns moveTurns = new Turns(currentTurns.getYaw(), currentTurns.getPitch());
            moveTurns.setYaw(MathHelper.lerp(Math.clamp(randomLerp(speed, speed + 0.2f), 0, 1), currentTurns.getYaw(), currentTurns.getYaw() + moveYaw) + yaw);
            moveTurns.setPitch(MathHelper.lerp(Math.clamp(randomLerp(speed, speed + 0.2f), 0, 1), currentTurns.getPitch(), currentTurns.getPitch() + movePitch) + pitch);

            return moveTurns;
        }
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }
}
