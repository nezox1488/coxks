package fun.rich.utils.math;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class Animation {
    private double value;
    private double target;
    private float speed;

    public Animation(float speed) {
        this.speed = speed;
    }

    public void update() {
        float deltaTime = MinecraftClient.getInstance().getRenderTickCounter().getLastFrameDuration();
        value = Calculate.interpolateSmooth(speed * deltaTime, (float) value, (float) target);
    }

    public void setTarget(double target) {
        this.target = target;
    }

    public double getValue() {
        return value;
    }

    public static class Calculate {
        public static float interpolateSmooth(float speed, float current, float target) {
            return current + (target - current) * MathHelper.clamp(speed, 0, 1);
        }
    }
}