package fun.rich.utils.features.aura.rotations;
/**
 * @author Sitoku
 * @since 3/3/2026
 */
import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.features.aura.warp.TurnsConnection;
import fun.rich.utils.features.aura.warp.TurnsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class RotationLogger implements QuickImports {

    private static final RotationLogger INSTANCE = new RotationLogger();

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneOffset.UTC);

    private volatile boolean enabled;
    private BufferedWriter out;
    private Path currentFile;

    private long tick;
    private long lastWriteMs;

    private float lastYaw;
    private float lastPitch;
    private float lastYawDelta;
    private float lastPitchDelta;

    private long stuckSinceMs;
    private float stuckYaw;
    private float stuckPitch;

    private RotationLogger() {}

    public static RotationLogger get() {
        return INSTANCE;
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized Path getCurrentFile() {
        return currentFile;
    }

    public synchronized void enable() {
        if (enabled) return;
        try {
            Path dir = Path.of("rotation");
            Files.createDirectories(dir);
            currentFile = dir.resolve("rotation_" + TS.format(Instant.now()) + ".log");
            out = Files.newBufferedWriter(currentFile, StandardCharsets.UTF_8);
            enabled = true;
            tick = 0;
            lastWriteMs = 0;
            stuckSinceMs = 0;
            lastYawDelta = 0f;
            lastPitchDelta = 0f;
            writeHeader();
        } catch (IOException e) {
            enabled = false;
            closeQuietly();
        }
    }

    public synchronized void disable() {
        if (!enabled) return;
        enabled = false;
        closeQuietly();
    }

    private void writeHeader() throws IOException {
        out.write("ts_ms tick aimMode yaw pitch dyaw dpitch speed_deg_s target dist");
        out.newLine();
        out.flush();
    }

    public void onRotationApplied(TurnsConnection controller, TurnsConstructor plan, Turns newAngle) {
        if (!enabled || newAngle == null) return;
        try {
            tick++;
            long nowMs = System.currentTimeMillis();
            float yaw = newAngle.getYaw();
            float pitch = newAngle.getPitch();

            float dyaw = MathHelper.wrapDegrees(yaw - lastYaw);
            float dpitch = pitch - lastPitch;
            float speedDegS = (float) (Math.hypot(dyaw, dpitch) * 20.0);

            String aimMode = safeAimMode();
            Entity target = plan != null ? plan.getEntity() : null;
            String targetName = target instanceof LivingEntity le ? safeName(le) : "-";
            double dist = target instanceof LivingEntity le ? safeDist(le) : -1.0;

            String flags = buildFlags(nowMs, dyaw, dpitch, speedDegS, plan, aimMode);

            writeLine(nowMs, tick, aimMode, yaw, pitch, dyaw, dpitch, speedDegS, targetName, dist, flags);

            lastYaw = yaw;
            lastPitch = pitch;
            lastYawDelta = dyaw;
            lastPitchDelta = dpitch;
            lastWriteMs = nowMs;
        } catch (Throwable ignored) {
        }
    }

    private String safeAimMode() {
        try {
            Aura aura = Aura.getInstance();
            if (aura == null || aura.getAimMode() == null) return "-";
            return String.valueOf(aura.getAimMode().getSelected());
        } catch (Throwable t) {
            return "-";
        }
    }

    private String safeName(LivingEntity e) {
        try {
            return e.getDisplayName() != null ? e.getDisplayName().getString() : e.getName().getString();
        } catch (Throwable t) {
            return "entity";
        }
    }

    private double safeDist(LivingEntity target) {
        try {
            if (mc.player == null) return -1.0;
            return mc.player.getPos().distanceTo(target.getPos());
        } catch (Throwable t) {
            return -1.0;
        }
    }

    private String buildFlags(long nowMs, float dyaw, float dpitch, float speedDegS, TurnsConstructor plan, String aimMode) {
        StringBuilder sb = new StringBuilder();

        if (speedDegS > 220.0f) {
            appendFlag(sb, "HIGH_SPEED", "скорость поворота слишком высокая; попробуй уменьшить maxYaw/maxPitch или усилить smoothing");
        }
        if (Math.abs(dpitch) > 25.0f) {
            appendFlag(sb, "PITCH_SPIKE", "резкий скачок pitch; зажми pitchSpeed или clamp шага pitch за тик");
        }
        if (Math.abs(MathHelper.wrapDegrees(dyaw)) < 0.02f && Math.abs(dpitch) < 0.02f && plan != null && plan.getEntity() != null) {
            if (stuckSinceMs == 0) {
                stuckSinceMs = nowMs;
                stuckYaw = lastYaw;
                stuckPitch = lastPitch;
            } else if (nowMs - stuckSinceMs > 1500L) {
                appendFlag(sb, "STUCK_LOOK", "залипание на угле при активном таргете; проверь reset/ticksUntilReset и ошибки накопления в ротации");
            }
        } else {
            stuckSinceMs = 0;
        }

        boolean oscillationYaw = Math.signum(dyaw) != 0f && Math.signum(lastYawDelta) != 0f && Math.signum(dyaw) != Math.signum(lastYawDelta) && Math.abs(dyaw) > 0.8f && Math.abs(lastYawDelta) > 0.8f;
        boolean oscillationPitch = Math.signum(dpitch) != 0f && Math.signum(lastPitchDelta) != 0f && Math.signum(dpitch) != Math.signum(lastPitchDelta) && Math.abs(dpitch) > 0.6f && Math.abs(lastPitchDelta) > 0.6f;
        if (oscillationYaw || oscillationPitch) {
            appendFlag(sb, "OSCILLATION", "дёрганье смена знака дельты уменьшай jitter/overshoot и добавь сглаживание шага");
        }

        if (aimMode != null && !aimMode.equals("-") && aimMode.toLowerCase().contains("spooky") && speedDegS > 160.0f) {
            appendFlag(sb, "SPOOKY_FAST", "скорость великовата снизь yaw/pitch speed или усили easing");
        }

        return sb.toString();
    }

    private void appendFlag(StringBuilder sb, String code, String hint) {
        if (sb.length() > 0) sb.append(" | ");
        sb.append(code).append(": ").append(hint);
    }

    private synchronized void writeLine(long nowMs, long tick, String aimMode, float yaw, float pitch, float dyaw, float dpitch, float speedDegS, String target, double dist, String flags) throws IOException {
        if (!enabled || out == null) return;
        out.write(nowMs + " " + tick + " " + aimMode + " " + fmt(yaw) + " " + fmt(pitch) + " " + fmt(dyaw) + " " + fmt(dpitch) + " " + fmt(speedDegS) + " " + target + " " + fmt(dist));
        out.newLine();
        if (flags != null && !flags.isEmpty()) {
            out.write("FLAG " + flags);
            out.newLine();
        }
        if (nowMs - lastWriteMs > 250L) {
            out.flush();
        }
    }

    private String fmt(double v) {
        return String.format(java.util.Locale.US, "%.3f", v);
    }

    private void closeQuietly() {
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {
        } finally {
            out = null;
            currentFile = null;
        }
    }
}

