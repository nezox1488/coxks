package fun.rich.utils.features.aura.rotations.impl;

import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.striking.StrikeManager;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fun.rich.Rich;

import java.security.SecureRandom;
import java.util.LinkedList;

public class SpookyTestAngle extends RotateConstructor {

    private static final SecureRandom RANDOM = new SecureRandom();

    // Динамические параметры (скорости, джиттер и т.д.)
    private float baseYawSpeed;
    private float basePitchSpeed;
    private float fastYawSpeed;
    private float fastPitchSpeed;
    private float yawThreshold;
    private float pitchThreshold;
    private float microJitterYaw;
    private float microJitterPitch;

    private final LinkedList<Float> yawHistory = new LinkedList<>();
    private final LinkedList<Float> pitchHistory = new LinkedList<>();
    private int historySize;

    private long lastMoveTime = 0;
    private long sessionStartTime = 0;
    private long lastProfileChange = 0;
    private long lastPauseTime = 0;
    private long pauseDuration = 0;
    private boolean isPaused = false;

    private int movePhase = 0;
    private float phaseProgress = 0;
    private float fatigueLevel = 0f;
    private int currentProfile = 0;
    private float profileBlend = 0f;
    private int nextProfile = 0;

    private float wanderYaw = 0f;
    private float wanderPitch = 0f;
    private long lastWanderUpdate = 0;

    private int tickCounter = 0;
    private int attackCounter = 0;
    private float lastTotalDelta = 0f;

    private float errorYaw = 0f;
    private float errorPitch = 0f;
    private long errorEndTime = 0;

    public SpookyTestAngle() {
        super("SpookyTest");
        initializeSession();
    }

    private void initializeSession() {
        sessionStartTime = System.currentTimeMillis();
        lastProfileChange = sessionStartTime;
        fatigueLevel = 0f;

        currentProfile = RANDOM.nextInt(4);
        nextProfile = (currentProfile + 1 + RANDOM.nextInt(3)) % 4;
        profileBlend = 0f;

        regenerateParameters();
    }

    private void regenerateParameters() {
        float profileFactor = MathHelper.lerp(profileBlend, getProfileFactor(currentProfile), getProfileFactor(nextProfile));
        float jitterFactor = MathHelper.lerp(profileBlend, getJitterFactor(currentProfile), getJitterFactor(nextProfile));

        baseYawSpeed = (37f + randomRange(-5f, 8f)) * profileFactor;
        basePitchSpeed = (18f + randomRange(-3f, 4f)) * profileFactor;
        fastYawSpeed = (40f + randomRange(-8f, 12f)) * profileFactor;
        fastPitchSpeed = (20f + randomRange(-4f, 6f)) * profileFactor;

        yawThreshold = 40f + randomRange(-10f, 15f);
        pitchThreshold = 18f + randomRange(-5f, 7f);

        microJitterYaw = (1.5f + randomRange(-0.5f, 1.0f)) * jitterFactor;
        microJitterPitch = (0.7f + randomRange(-0.3f, 0.5f)) * jitterFactor;

        historySize = 3 + RANDOM.nextInt(3);
    }

    private float getProfileFactor(int profile) {
        return switch (profile) {
            case 0 -> 1.25f + randomRange(-0.1f, 0.15f);
            case 1 -> 0.75f + randomRange(-0.1f, 0.1f);
            case 2 -> 1.0f + randomRange(-0.15f, 0.15f);
            case 3 -> 0.85f + randomRange(-0.1f, 0.1f);
            default -> 1.0f;
        };
    }

    private float getJitterFactor(int profile) {
        return switch (profile) {
            case 0 -> 0.8f + randomRange(-0.1f, 0.2f);
            case 1 -> 0.6f + randomRange(-0.1f, 0.15f);
            case 2 -> 1.8f + randomRange(-0.2f, 0.4f);
            case 3 -> 1.0f + randomRange(-0.15f, 0.2f);
            default -> 1.0f;
        };
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        long currentTime = System.currentTimeMillis();

        tickCounter++;

        updateDynamicState(currentTime);

        if (isPaused) {
            if (currentTime < lastPauseTime + pauseDuration) {
                float pauseJitterY = randomRange(-0.3f, 0.3f) * (1f + fatigueLevel);
                float pauseJitterP = randomRange(-0.15f, 0.15f) * (1f + fatigueLevel);
                return new Turns(
                        currentAngle.getYaw() + pauseJitterY,
                        MathHelper.clamp(currentAngle.getPitch() + pauseJitterP, -90f, 90f)
                ).adjustSensitivity();
            }
            isPaused = false;
        }

        if (shouldPause(currentTime)) {
            isPaused = true;
            lastPauseTime = currentTime;
            pauseDuration = 50 + RANDOM.nextInt(200);
            return currentAngle;
        }

        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float totalDelta = (float) Math.hypot(yawDelta, pitchDelta);

        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);
        if (canAttack) attackCounter++;

        updateMovePhase(totalDelta, canAttack);
        updateRandomErrors(currentTime, totalDelta);
        updateWander(currentTime, canAttack);

        float fatigueMult = 1f - (fatigueLevel * 0.25f);
        float yawSpeed = calculateYawSpeed(yawDelta, totalDelta, canAttack) * fatigueMult;
        float pitchSpeed = calculatePitchSpeed(pitchDelta, totalDelta, canAttack) * fatigueMult;

        float easedYawDelta = applyHumanEasing(yawDelta, yawSpeed, totalDelta);
        float easedPitchDelta = applyHumanEasing(pitchDelta, pitchSpeed, totalDelta);

        float fatigueJitterMult = 1f + (fatigueLevel * 0.8f);
        float jitterYaw = calculateMicroJitter(microJitterYaw * fatigueJitterMult, canAttack, currentTime, true);
        float jitterPitch = calculateMicroJitter(microJitterPitch * fatigueJitterMult, canAttack, currentTime, false);

        easedYawDelta = smoothWithHistory(easedYawDelta, yawHistory);
        easedPitchDelta = smoothWithHistory(easedPitchDelta, pitchHistory);

        if (canAttack && totalDelta < 10.0f) {
            float overshoot = calculateOvershoot(totalDelta);
            easedYawDelta *= (1.0f + overshoot);
            easedPitchDelta *= (1.0f + overshoot);
        }

        easedYawDelta += errorYaw + wanderYaw;
        easedPitchDelta += errorPitch + wanderPitch;

        Turns result = new Turns(
                currentAngle.getYaw() + easedYawDelta + jitterYaw,
                MathHelper.clamp(currentAngle.getPitch() + easedPitchDelta + jitterPitch, -90.0f, 90.0f)
        );

        lastMoveTime = currentTime;
        lastTotalDelta = totalDelta;

        return result.adjustSensitivity();
    }

    private void updateDynamicState(long currentTime) {
        long sessionDuration = currentTime - sessionStartTime;
        fatigueLevel = Math.min(1f, sessionDuration / (1000f * 60f * (20f + randomRange(0, 8))));

        long profileInterval = 120000 + RANDOM.nextInt(90000);
        if (currentTime - lastProfileChange > profileInterval) {
            currentProfile = nextProfile;
            nextProfile = (currentProfile + 1 + RANDOM.nextInt(3)) % 4;
            profileBlend = 0f;
            lastProfileChange = currentTime;
            regenerateParameters();
        } else {
            profileBlend = MathHelper.clamp(profileBlend + 0.01f, 0f, 1f);
        }
    }

    private boolean shouldPause(long currentTime) {
        if (fatigueLevel < 0.2f) return false;
        long timeSinceLastMove = currentTime - lastMoveTime;
        long minInterval = (long) (300 + fatigueLevel * 400);
        long maxInterval = (long) (800 + fatigueLevel * 1200);
        if (timeSinceLastMove < minInterval) return false;

        float pauseChance = 0.1f + fatigueLevel * 0.35f;
        if (tickCounter % 10 == 0 && RANDOM.nextFloat() < pauseChance) {
            return true;
        }
        return false;
    }

    private void updateMovePhase(float totalDelta, boolean canAttack) {
        if (totalDelta < 2.0f && !canAttack) {
            movePhase = 0;
            phaseProgress = 0;
            return;
        }

        if (canAttack) {
            if (movePhase == 0 || movePhase == 2) {
                movePhase = 1;
                phaseProgress = 0;
            }
        } else {
            if (movePhase == 1 && phaseProgress > 0.7f) {
                movePhase = 2;
                phaseProgress = 0;
            }
        }

        float phaseSpeed = switch (movePhase) {
            case 0 -> 0.03f;
            case 1 -> 0.08f;
            case 2 -> 0.05f;
            default -> 0.04f;
        };

        phaseProgress = MathHelper.clamp(phaseProgress + phaseSpeed, 0f, 1f);
    }

    private void updateRandomErrors(long currentTime, float totalDelta) {
        if (currentTime < errorEndTime) return;
        if (totalDelta < 5.0f) return;
        float errorChance = 0.03f + fatigueLevel * 0.07f;
        if (RANDOM.nextFloat() < errorChance) {
            float errorStrength = 0.5f + fatigueLevel * 1.5f;
            errorYaw = randomRange(-errorStrength * 2f, errorStrength * 2f);
            errorPitch = randomRange(-errorStrength, errorStrength);
            long duration = 80 + RANDOM.nextInt(120);
            errorEndTime = currentTime + duration;
        } else {
            errorYaw *= 0.9f;
            errorPitch *= 0.9f;
        }
    }

    private void updateWander(long currentTime, boolean canAttack) {
        long interval = canAttack ? 60 : 120;
        if (currentTime - lastWanderUpdate > interval) {
            lastWanderUpdate = currentTime;
            float wanderStrength = 0.3f + fatigueLevel * 0.6f;
            wanderYaw += randomRange(-wanderStrength, wanderStrength);
            wanderPitch += randomRange(-wanderStrength * 0.7f, wanderStrength * 0.7f);
            wanderYaw *= 0.95f;
            wanderPitch *= 0.95f;
        }
    }

    private float calculateYawSpeed(float yawDelta, float totalDelta, boolean canAttack) {
        float baseSpeed = MathHelper.lerp(phaseProgress, baseYawSpeed, fastYawSpeed);
        float speed = baseSpeed;
        float absYaw = Math.abs(yawDelta);
        if (absYaw > yawThreshold) {
            float factor = 1.0f + (absYaw - yawThreshold) / 90.0f;
            speed *= factor;
        }
        if (canAttack) {
            speed *= 1.1f + (float) Math.min(0.25, totalDelta / 90.0);
        }
        speed *= 1.0f + fatigueLevel * 0.15f;
        return speed;
    }

    private float calculatePitchSpeed(float pitchDelta, float totalDelta, boolean canAttack) {
        float baseSpeed = MathHelper.lerp(phaseProgress, basePitchSpeed, fastPitchSpeed);
        float speed = baseSpeed;
        float absPitch = Math.abs(pitchDelta);
        if (absPitch > pitchThreshold) {
            float factor = 1.0f + (absPitch - pitchThreshold) / 45.0f;
            speed *= factor;
        }
        if (canAttack) {
            speed *= 1.05f + (float) Math.min(0.2, totalDelta / 120.0);
        }
        speed *= 1.0f + fatigueLevel * 0.1f;
        return speed;
    }

    private float applyHumanEasing(float delta, float speed, float totalDelta) {
        if (Math.abs(delta) < 0.001f || totalDelta < 0.001f) return 0f;
        float t = MathHelper.clamp(Math.abs(delta) / totalDelta, 0f, 1f);
        float easedT = t * t * (3 - 2 * t);
        float maxStep = speed * 0.016f;
        return MathHelper.clamp(delta * easedT, -maxStep, maxStep);
    }

    private float calculateMicroJitter(float intensity, boolean canAttack, long currentTime, boolean yaw) {
        float baseFreq = yaw ? 0.65f : 0.8f;
        float attackFactor = canAttack ? 1.3f : 1.0f;
        float fatigueFactor = 1.0f + fatigueLevel * 0.5f;
        float time = (currentTime % 10000L) / 1000.0f;
        float phase1 = time * baseFreq * 6.28318f;
        float phase2 = time * baseFreq * 1.37f * 6.28318f;
        float phase3 = time * baseFreq * 0.73f * 6.28318f;
        float jitter = (MathHelper.sin(phase1) + MathHelper.sin(phase2) * 0.7f + MathHelper.sin(phase3) * 0.4f) / 2.1f;
        jitter += (RANDOM.nextFloat() - 0.5f) * 0.25f;
        return jitter * intensity * attackFactor * fatigueFactor;
    }

    private float smoothWithHistory(float value, LinkedList<Float> history) {
        history.add(value);
        while (history.size() > historySize) {
            history.removeFirst();
        }
        float sum = 0f;
        float weightSum = 0f;
        float weight = 1f;
        for (int i = history.size() - 1; i >= 0; i--) {
            sum += history.get(i) * weight;
            weightSum += weight;
            weight *= 0.7f;
        }
        return sum / Math.max(1e-4f, weightSum);
    }

    private float calculateOvershoot(float totalDelta) {
        float norm = MathHelper.clamp(totalDelta / 25.0f, 0f, 1f);
        float base = 0.06f + norm * 0.12f;
        return base * (1.0f + (RANDOM.nextFloat() - 0.5f) * 0.3f);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }

    private float randomRange(float min, float max) {
        return min + RANDOM.nextFloat() * (max - min);
    }
}

