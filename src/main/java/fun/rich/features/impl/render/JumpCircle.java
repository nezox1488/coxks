package fun.rich.features.impl.render;


import fun.rich.events.render.WorldRenderEvent;
import fun.rich.utils.display.geometry.Render3D;
import fun.rich.utils.math.Counter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.common.repository.friend.FriendUtils;
import fun.rich.common.animation.Animation;
import fun.rich.common.animation.Direction;
import fun.rich.common.animation.implement.Decelerate;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.events.player.TickEvent;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.render.VertexFormat.DrawMode.QUADS;
import static net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class JumpCircle extends Module {

    private final List<Circle> circles = new ArrayList<>();
    final Identifier circleTexture = Identifier.of("textures/circle2.png");
    boolean wasOnGround = true;

    public JumpCircle() {
        super("JumpCircle", "Jump Circle", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onUpdate(TickEvent event) {
        if (mc.player == null) return;

        boolean isOnGround = mc.player.isOnGround();

        if (wasOnGround && !isOnGround) {
            Vec3d pos = new Vec3d(
                    mc.player.getX(),
                    Math.floor(mc.player.getY()) + 0.001,
                    mc.player.getZ()
            );
            circles.add(new Circle(pos, new Counter()));
        }

        wasOnGround = isOnGround;

        circles.removeIf(c -> c.timer.passedMs(3000));
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        renderCircles();
    }

    private void renderCircles() {
        if (circles.isEmpty()) return;

        for (Circle circle : circles) {
            renderSingleCircle(circle);
        }
    }

    private void renderSingleCircle(Circle circle) {
        float lifeTime = (float) circle.timer.getPassedTimeMs();
        float maxTime = 3000f;
        float progress = lifeTime / maxTime;

        if (progress >= 1f) return;

        float scale = progress * 2f;
        float alpha = 1f - (progress * progress);

        int baseColor = ColorAssist.fade((int)(progress * 360f));
        int color = ColorAssist.multAlpha(baseColor, alpha);

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d cameraPos = camera.getPos();
        Vec3d circlePos = circle.pos();

        MatrixStack matrixStack = new MatrixStack();

        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        matrixStack.translate(circlePos.x - cameraPos.x, circlePos.y - cameraPos.y, circlePos.z - cameraPos.z);

        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        MatrixStack.Entry entry = matrixStack.peek();
        Vector4i colors = new Vector4i(color, color, color, color);

        Render3D.drawTexture(entry, circleTexture, -scale/2, -scale/2, scale, scale, colors, true);
    }

    public record Circle(Vec3d pos, Counter timer) {}
}