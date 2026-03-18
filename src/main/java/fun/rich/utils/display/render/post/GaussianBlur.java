package fun.rich.utils.display.render.post;

import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.render.util.CyclicStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;



public class GaussianBlur implements QuickImports {
    private static final CyclicStack<Framebuffer> stack = new CyclicStack<>() {{
        add(new SimpleFramebuffer(1, 1, false));
        add(new SimpleFramebuffer(1, 1, false));
    }};

    private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("core/gaussian_blur"), VertexFormats.POSITION, Defines.EMPTY);

    public static void applyBlurEffect(final Framebuffer read, final Framebuffer write,
                                       int countOfPasses, float scaleMultiplier) {
        resizeFramebuffers();

        Framebuffer readBuffer = read;
        for (int i = 0; i < countOfPasses; i++) {
            Framebuffer wroteBuffer = stack.next();
            executePass(readBuffer, wroteBuffer, scaleMultiplier, i % 2 == 0 ? 1f : 0f);
            readBuffer = wroteBuffer;
        }

        write.beginWrite(false);
        readBuffer.beginRead();

        RenderSystem.setShader(ShaderProgramKeys.BLIT_SCREEN);

        drawQuad();

        readBuffer.endRead();
        write.endWrite();

        mc.getFramebuffer().beginWrite(false);
    }

    private static void resizeFramebuffers() {
        for (Framebuffer framebuffer : stack) {
            if (framebuffer.textureWidth != mc.getFramebuffer().textureWidth || framebuffer.textureHeight != mc.getFramebuffer().textureHeight) {
                framebuffer.resize(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            }
        }
    }

    private static void executePass(Framebuffer read, Framebuffer write, float scaleMultiplier, float offsetDirection) {
        write.beginWrite(false);
        read.beginRead();

//        RenderSystem.setShaderTexture(0, read.getColorAttachment());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ShaderProgram shaderProgram = RenderSystem.setShader(SHADER_KEY);
        shaderProgram.getUniform("scaleMultiplier").set(scaleMultiplier);
        shaderProgram.getUniform("offsetDirection").set(offsetDirection);

        drawQuad();

        read.endRead();
        write.endWrite();

        RenderSystem.disableBlend();
    }

    private static void drawQuad() {
        float width = mc.getWindow().getScaledWidth();
        float height = mc.getWindow().getScaledHeight();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        builder.vertex(0, 0, 0);
        builder.vertex(0, height, 0);
        builder.vertex(width, height, 0);
        builder.vertex(width, 0, 0);

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }
}
