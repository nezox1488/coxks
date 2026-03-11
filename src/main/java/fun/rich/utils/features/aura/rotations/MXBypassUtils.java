package fun.rich.utils.features.aura.rotations;

import net.minecraft.util.math.MathHelper;

import java.security.SecureRandom;

public final class MXBypassUtils {
    private static final SecureRandom RNG = new SecureRandom();

    public static float sanitizePitch(float pitch) {
        float p = MathHelper.clamp(pitch, -89.99f, 89.99f);
        if (Math.abs(p) < 0.001f || Math.abs(p % 0.01f) < 0.0005f) {
            p += (RNG.nextFloat() * 0.008f - 0.004f);
        }
        return p;
    }

    public static float addGCDNoise(float value) {
        return value + (RNG.nextFloat() * 0.02f - 0.01f);
    }

    public static float deltaNoise() {
        return (RNG.nextFloat() * 0.015f - 0.0075f);
    }
}
