package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.features.aura.point.Vector;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.interactions.simulate.PlayerSimulation;
import fun.rich.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fun.rich.utils.math.time.TimerUtil;
import fun.rich.Rich;
import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.features.aura.striking.StrikeManager;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.features.aura.utils.MathAngle;

import java.security.SecureRandom;

public class FTAngle extends RotateConstructor {
    private int swingCount = 0;
    private boolean hasSwungTwice = false;
    private boolean hasSwung = false;
    private boolean disableRotation = false;
    TimerUtil timer = new TimerUtil();

    public FTAngle() {
        super("FunTime");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        if (disableRotation) {
            return new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        }
        Aura aura = Aura.getInstance();

        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        int count = attackHandler.getCount();
        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        if (entity != null && attackHandler.getAttackTimer().finished(400)) {
            float speed = randomLerp(0.65F, 0.85F);
            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float jitterYaw = (float) (randomLerp(15, 13) * Math.sin(System.currentTimeMillis() / 60D));
            float jitterPitch = (float) (randomLerp(4, 9) * Math.sin(System.currentTimeMillis() / 60D));

            Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed),
                    currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
            moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed),
                    currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);

            return moveAngle;
        } else {
            float speed = attackHandler.getAttackTimer().finished(450) ? 0.15F : 0.02F;
            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float jitterYaw = (float) (randomLerp(15, 13) * Math.sin(System.currentTimeMillis() / 70D));
            float jitterPitch = (float) (randomLerp(4, 9) * Math.sin(System.currentTimeMillis() / 60D));

            if ((!aura.isState() || aura.getTarget() == null) && attackHandler.getAttackTimer().finished(1200)) {
                jitterYaw = 0;
                speed = 0.35F;
                jitterPitch = 0;
            }

            Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed),
                    currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
            moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed),
                    currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);

            return moveAngle;
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