package baritone.api.utils;

import java.util.Random;

public class Rotation {

    private final float yaw;
    private final float pitch;
    private static final Random RND = new Random();

    public Rotation(float yaw, float pitch) {
        // Внедряем микро-шум прямо при создании объекта.
        // Это ломает проверку: invalidDistinct = distinct < 15
        float noiseYaw = (RND.nextFloat() - 0.5f) * 0.0001f;
        float noisePitch = (RND.nextFloat() - 0.5f) * 0.0001f;

        this.yaw = yaw + noiseYaw;
        this.pitch = pitch + noisePitch;

        if (Float.isInfinite(this.yaw) || Float.isNaN(this.yaw) || Float.isInfinite(this.pitch) || Float.isNaN(this.pitch)) {
            throw new IllegalStateException(this.yaw + " " + this.pitch);
        }
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public Rotation add(Rotation other) {
        // Используем базовый конструктор, который уже добавит свой шум сверху
        return new Rotation(
                this.yaw + other.yaw,
                this.pitch + other.pitch
        );
    }

    public Rotation subtract(Rotation other) {
        return new Rotation(
                this.yaw - other.yaw,
                this.pitch - other.pitch
        );
    }

    public Rotation clamp() {
        return new Rotation(
                this.yaw,
                clampPitch(this.pitch)
        );
    }

    public Rotation normalize() {
        return new Rotation(
                normalizeYaw(this.yaw),
                this.pitch
        );
    }

    public Rotation normalizeAndClamp() {
        return new Rotation(
                normalizeYaw(this.yaw),
                clampPitch(this.pitch)
        );
    }

    public Rotation withPitch(float pitch) {
        return new Rotation(this.yaw, pitch);
    }

    public boolean isReallyCloseTo(Rotation other) {
        // Увеличиваем порог "близости", чтобы античит не видел четких паттернов
        return yawIsReallyClose(other) && Math.abs(this.pitch - other.pitch) < 0.05;
    }

    public boolean yawIsReallyClose(Rotation other) {
        float yawDiff = Math.abs(normalizeYaw(yaw) - normalizeYaw(other.yaw));
        return (yawDiff < 0.05 || yawDiff > 359.95);
    }

    public static float clampPitch(float pitch) {
        return Math.max(-90, Math.min(90, pitch));
    }

    public static float normalizeYaw(float yaw) {
        float newYaw = yaw % 360F;
        if (newYaw < -180F) {
            newYaw += 360F;
        }
        if (newYaw > 180F) {
            newYaw -= 360F;
        }
        return newYaw;
    }

    @Override
    public String toString() {
        return "Yaw: " + yaw + ", Pitch: " + pitch;
    }
}
