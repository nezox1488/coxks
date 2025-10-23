package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.features.aura.point.Vector;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
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

        if (entity != null) {
<<<<<<< Updated upstream
            float speed = 0.8F;
            StopWatch attackTimer = attackHandler.getAttackTimer();
=======
            float speed = 0.50F;
>>>>>>> Stashed changes
            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed + 0.2F),
                    currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
            moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed + 0.2F),
                    currentAngle.getPitch(), currentAngle.getPitch() + movePitch));

            return moveAngle;
        } else {
<<<<<<< Updated upstream
            StopWatch attackTimer = attackHandler.getAttackTimer();
            int suck = count % 3;
            float speed =  attackTimer.finished(470)
                    ? 0.15f
                    : -0.1F;
            float random = attackTimer.elapsedTime() / 40 + (count % 6);

            Turns randomAngle = switch (suck) {
                case 0 -> new Turns((float) Math.cos(random), (float) Math.sin(random));
                case 1 -> new Turns((float) Math.sin(random), (float) Math.cos(random));
                case 2 -> new Turns((float) Math.sin(random), (float) -Math.cos(random));
                default -> new Turns((float) -Math.cos(random), (float) Math.sin(random));
            };

            float yaw = randomLerp(14, 18) * randomAngle.getYaw();
            float pitch = randomLerp(6, 10) * randomAngle.getPitch();

            if ((!aura.isState() || Aura.getInstance().getTarget() == null) && attackHandler.getAttackTimer().finished(350)) {
                speed = 0.4F;
                yaw = 0;
                pitch = 0;
            }

=======
            float speed = attackHandler.getAttackTimer().finished(1050) ? 0.31F : 0.02F;
>>>>>>> Stashed changes
            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

<<<<<<< Updated upstream
=======
            float jitterYaw = (float) (randomLerp(21, 21) * Math.sin(System.currentTimeMillis() / 35D));
            float jitterPitch = (float) (randomLerp(6, 16) * Math.sin(System.currentTimeMillis() / 45D));

            if ((!aura.isState() || entity == null) && attackHandler.getAttackTimer().finished(2500)) {
                jitterYaw = 0;
                jitterPitch = 0;
                speed = 0.5F;
            }

>>>>>>> Stashed changes
            Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(
                    MathHelper.lerp(Math.clamp(randomLerp(speed, speed + 0.2F), 0, 1),
                            currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + yaw
            );
            moveAngle.setPitch(
                    MathHelper.lerp(Math.clamp(randomLerp(speed, speed + 0.2F), 0, 1),
                            currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + pitch
            );

            if (count > 0 && count % 50 == 0 && !attackHandler.getAttackTimer().finished(350)) {
                moveAngle.setPitch(
                        MathHelper.lerp(Math.clamp(0.874F, 0, 1),
                                currentAngle.getPitch(), -90)
                );
                if (!attackHandler.getAttackTimer().finished(200)) {
                    mc.player.swingHand(Hand.MAIN_HAND);

                }
            }

            return moveAngle;
        }
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.05, 0.1, 0.02);
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }
}