package fun.rich.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
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
import fun.rich.events.render.DrawEvent;

import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.client.render.VertexFormat.DrawMode.QUADS;
import static net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Arrows extends Module {
    Identifier iconId = Identifier.of("textures/features/arrows/arrow.png");
    Animation radiusAnim = new Decelerate().setMs(150).setValue(6);

    SliderSettings radiusSetting = new SliderSettings("Радиус", "Радиус стрелок")
            .setValue(50).range(30, 100);

    SliderSettings sizeSetting = new SliderSettings("Размер", "Размер стрелок")
            .setValue(10).range(8, 20);

    public Arrows() {
        super("Arrows", "Arrows", ModuleCategory.RENDER);
        setup(radiusSetting, sizeSetting);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) return;
        radiusAnim.setDirection(mc.player.isSprinting() ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (mc.player == null || mc.world == null) return;

        List<AbstractClientPlayerEntity> players = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .collect(Collectors.toList());

        if (players.isEmpty()) return;

        MatrixStack matrix = e.getDrawContext().getMatrices();
        float middleW = mc.getWindow().getScaledWidth() / 2f;
        float middleH = mc.getWindow().getScaledHeight() / 2f;
        float posY = middleH - radiusSetting.getValue() - radiusAnim.getOutput().floatValue();
        float size = sizeSetting.getValue();

        if (!mc.options.hudHidden && mc.options.getPerspective().equals(Perspective.FIRST_PERSON)) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShaderTexture(0, iconId);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = tessellator.begin(QUADS, POSITION_TEXTURE_COLOR);

            for (AbstractClientPlayerEntity player : players) {
                int color = FriendUtils.isFriend(player) ? ColorAssist.getFriendColor() : ColorAssist.getClientColor();
                float yaw = getRotations(player) - mc.player.getYaw();

                matrix.push();
                matrix.translate(middleW, middleH, 0.0F);
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
                matrix.translate(-middleW, -middleH, 0.0F);

                Matrix4f mat = matrix.peek().getPositionMatrix();
                buffer.vertex(mat, middleW - (size / 2f), posY + size, 0).texture(0f, 1f).color(ColorAssist.multAlpha(ColorAssist.multDark(color, 0.4F), 0.5F));
                buffer.vertex(mat, middleW + size / 2f, posY + size, 0).texture(1f, 1f).color(ColorAssist.multAlpha(ColorAssist.multDark(color, 0.4F), 0.5F));
                buffer.vertex(mat, middleW + size / 2f, posY, 0).texture(1f, 0).color(color);
                buffer.vertex(mat, middleW - (size / 2f), posY, 0).texture(0, 0).color(color);

                matrix.pop();
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    public static float getRotations(Entity entity) {
        double dx = Calculate.interpolate(entity.prevX, entity.getX()) - Calculate.interpolate(mc.player.prevX, mc.player.getX());
        double dz = Calculate.interpolate(entity.prevZ, entity.getZ()) - Calculate.interpolate(mc.player.prevZ, mc.player.getZ());
        return (float) -(Math.toDegrees(Math.atan2(dx, dz)));
    }
}
