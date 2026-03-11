package fun.rich.utils.display.render.post;

import fun.rich.utils.display.interfaces.QuickImports;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;


import java.util.ArrayList;
import java.util.List;

public class KawaseBlur implements QuickImports {
    public static final KawaseBlur INSTANCE = new KawaseBlur();
    private static final ShaderProgramKey upShader = new ShaderProgramKey(Identifier.of("minecraft", "core/blur/upscale"), VertexFormats.POSITION, Defines.EMPTY);
    private static final ShaderProgramKey downShader = new ShaderProgramKey(Identifier.of("minecraft", "core/blur/downscale"), VertexFormats.POSITION, Defines.EMPTY);

    public final List<SimpleFramebuffer> fbos = new ArrayList<>();

    private int steps;

    private void checkupFbo() {
        for (SimpleFramebuffer fbo : fbos) {
            if (mc.getFramebuffer().textureWidth != fbo.textureWidth || mc.getFramebuffer().textureHeight != fbo.textureHeight) {
                fbo.resize(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            }
        }
    }

    public void recreate() {
        fbos.forEach(Framebuffer::delete);
        fbos.clear();

        for (int i = 0; i <= steps; i++) {
            fbos.add(createFbo());
        }
    }

    public void render(MatrixStack matrixStack, int steps, float offset) {
        if (this.steps != steps) {
            this.steps = steps;
            recreate();
        }

        checkupFbo();

        int actualPasses = Math.max(fbos.size() - 1, 1);

        applyBlurPass(matrixStack, downShader, mc.getFramebuffer(), fbos.getFirst(), 0, offset);

        for (int i = 0; i < actualPasses; i++) {
            applyBlurPass(matrixStack, downShader, fbos.get(i), fbos.get(i + 1), i + 1, offset);
        }

        for (int i = actualPasses; i > 0; i--) {
            applyBlurPass(matrixStack, upShader, fbos.get(i), fbos.get(i - 1), i, offset);
        }

        mc.getFramebuffer().beginWrite(false);
    }

    private void applyBlurPass(MatrixStack matrixStack, ShaderProgramKey shaderKey, Framebuffer source, Framebuffer destination, int pass, float offset) {
        destination.beginWrite(false);

        RenderSystem.setShaderTexture(0, source.getColorAttachment());

        ShaderProgram shader = RenderSystem.setShader(shaderKey);

        shader.getUniform("uHalfTexelSize").set(0.5f / (float) source.textureWidth, 0.5f / (float) source.textureHeight);
        shader.getUniform("uOffset").set(offset * (pass / (float) steps));

        drawFullQuad(matrixStack.peek().getPositionMatrix());
        destination.endWrite();
    }

    private void drawFullQuad(Matrix4f matrix4f) {
        float width = mc.getWindow().getScaledWidth();
        float height = mc.getWindow().getScaledHeight();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        builder.vertex(matrix4f, 0, 0, 0);
        builder.vertex(matrix4f, 0, height, 0);
        builder.vertex(matrix4f, width, height, 0);
        builder.vertex(matrix4f, width, 0, 0);

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private SimpleFramebuffer createFbo() {
        return new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false);
    }
}
