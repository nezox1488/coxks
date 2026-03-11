package fun.rich.utils.display.render.geometry;
/**
 * @author Sitoku
 * @since 3/3/2026
 */
import fun.rich.common.animation.inovated.EasingList;
import fun.rich.features.impl.render.TargetESP;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.math.projection.Projection;
import fun.rich.events.render.WorldRenderEvent;
import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class Render3D implements QuickImports {
    private final Map<VoxelShape, Pair<List<Box>, List<Line>>> SHAPE_OUTLINES = new HashMap<>();
    private final Map<VoxelShape, List<Box>> SHAPE_BOXES = new HashMap<>();
    public final List<Texture> TEXTURE_DEPTH = new ArrayList<>();
    public final List<Texture> TEXTURE = new ArrayList<>();
    public final List<Line> LINE_DEPTH = new ArrayList<>();
    public final List<Line> LINE = new ArrayList<>();
    public final List<Quad> QUAD_DEPTH = new ArrayList<>();
    public final List<Quad> QUAD = new ArrayList<>();
    @Setter public Matrix4f lastProjMat = new Matrix4f();
    @Setter public MatrixStack.Entry lastWorldSpaceMatrix = new MatrixStack().peek();
    private final Identifier captureId = Identifier.of("textures/capture1.png"), bloom = Identifier.of("textures/bloom.png"), snow = Identifier.of("textures/show1.png");
    public final List<Crystal> crystalList = new ArrayList<>();

    private static class Crystal {
        private final Entity entity;
        private final Vec3d position;
        private final Vec3d rotation;
        private final float size;
        private final float rotationSpeed;

        public Crystal(Entity entity, Vec3d position, Vec3d rotation) {
            this.entity = entity;
            this.position = position;
            this.rotation = rotation;
            this.size = 0.09f;
            this.rotationSpeed = 0.5f + (float)(Math.random() * 1.5f);
        }

        public void render(MatrixStack ms) {
            ms.push();
            ms.translate(position.x, position.y, position.z);
            float pulsation = 1.0f + (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.1f);
            ms.scale(pulsation, pulsation, pulsation);
            float selfRotation = (System.currentTimeMillis() % 36000) / 100.0f * rotationSpeed;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y + selfRotation));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            int baseColor = ColorAssist.fade(90);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(ms, baseColor, 0.2f, true);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(ms, baseColor, 0.3f, true);
            drawCrystal(ms, baseColor, 0.8f, false);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            ms.pop();
        }

        private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
                    filled ? VertexFormat.DrawMode.TRIANGLES : VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR
            );
            float s = size;
            float h_prism = size * 1f;
            float h_pyramid = size * 1.5f;
            int numSides = 8;
            List<Vec3d> topVertices = new ArrayList<>();
            List<Vec3d> bottomVertices = new ArrayList<>();
            for (int i = 0; i < numSides; i++) {
                float angle = (float) (2 * Math.PI * i / numSides);
                float x = (float) (s * Math.cos(angle));
                float z = (float) (s * Math.sin(angle));
                topVertices.add(new Vec3d(x, h_prism / 2, z));
                bottomVertices.add(new Vec3d(x, -h_prism / 2, z));
            }
            Vec3d vTop = new Vec3d(0, h_prism / 2 + h_pyramid, 0);
            Vec3d vBottom = new Vec3d(0, -h_prism / 2 - h_pyramid, 0);
            int finalColor = ColorAssist.setAlpha(baseColor, (int) (alpha * 255));
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                Vec3d v3 = topVertices.get((i + 1) % numSides);
                Vec3d v4 = topVertices.get(i);
                drawQuad(ms, bufferBuilder, v1, v2, v3, v4, finalColor, filled);
            }
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = topVertices.get(i);
                Vec3d v2 = topVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vTop, v1, v2, finalColor, filled);
            }
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vBottom, v2, v1, finalColor, filled);
            }
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }

        private void drawTriangle(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, boolean filled) {
            if (filled) {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            } else {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            }
        }

        private void drawQuad(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, int color, boolean filled) {
            if (filled) {
                drawTriangle(ms, bb, v1, v2, v3, color, true);
                drawTriangle(ms, bb, v1, v3, v4, color, true);
            } else {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v4.x, (float)v4.y, (float)v4.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v4.x, (float)v4.y, (float)v4.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            }
        }
    }


    public void drawEntity(Entity entity, Vec3d pos, float yaw, int alpha, MatrixStack matrices, float tickDelta) {
        if (!(entity instanceof LivingEntity)) return;
        LivingEntity livingEntity = (LivingEntity) entity;
        matrices.push();
        matrices.translate(pos.x, pos.y, pos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.scale(1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        if (renderer != null) {
            int light = renderer.getLight(livingEntity, tickDelta);
            VertexConsumerProvider vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
            EntityRenderState renderState = renderer.getAndUpdateRenderState(livingEntity, tickDelta);
            if (renderState != null) {
                renderer.render(renderState, matrices, vertexConsumers, light);
            }
            ((VertexConsumerProvider.Immediate)vertexConsumers).draw();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        matrices.pop();
    }

    public void onWorldRender(WorldRenderEvent e) {
        if (!TEXTURE.isEmpty()) {
            Set<Identifier> identifiers = TEXTURE.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            identifiers.forEach(id -> {
                RenderSystem.setShaderTexture(0, id);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                TEXTURE.stream().filter(texture -> texture.id.equals(id)).forEach(tex -> quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            TEXTURE.clear();
        }
        if (!TEXTURE_DEPTH.isEmpty()) {
            Set<Identifier> identifiers = TEXTURE_DEPTH.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            identifiers.forEach(id -> {
                RenderSystem.setShaderTexture(0, id);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                TEXTURE_DEPTH.stream().filter(texture -> texture.id.equals(id)).forEach(tex -> quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            TEXTURE_DEPTH.clear();
        }
        if (!LINE.isEmpty()) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            Set<Float> widths = LINE.stream().map(line -> line.width).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            widths.forEach(width -> {
                RenderSystem.lineWidth(width);
                BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
                LINE.stream().filter(line -> line.width == width).forEach(line -> vertexLine(line.entry, buffer, line.start.toVector3f(), line.end.toVector3f(), line.colorStart, line.colorEnd));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            LINE.clear();
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
        if (!QUAD.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            QUAD.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            QUAD.clear();
        }
        if (!LINE_DEPTH.isEmpty()) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            Set<Float> widths = LINE_DEPTH.stream().map(line -> line.width).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            widths.forEach(width -> {
                RenderSystem.lineWidth(width);
                BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
                LINE_DEPTH.stream().filter(line -> line.width == width).forEach(line -> vertexLine(line.entry, buffer, line.start.toVector3f(), line.end.toVector3f(), line.colorStart, line.colorEnd));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            LINE_DEPTH.clear();
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
        if (!QUAD_DEPTH.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            QUAD_DEPTH.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            QUAD_DEPTH.clear();
        }
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
        drawShape(blockPos, voxelShape, color, width, true, false);
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        if (SHAPE_BOXES.containsKey(voxelShape)) {
            SHAPE_BOXES.get(voxelShape).forEach(box -> {
                box = box.offset(blockPos);
                if (Projection.canSee(box)) drawBox(box, color, width, true, fill, depth);
            });
            return;
        }
        SHAPE_BOXES.put(voxelShape, voxelShape.getBoundingBoxes());
    }

    public void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        Vec3d vec3d = Vec3d.of(blockPos);
        if (Projection.canSee(new Box(blockPos))) {
            if (SHAPE_OUTLINES.containsKey(voxelShape)) {
                Pair<List<Box>, List<Line>> pair = SHAPE_OUTLINES.get(voxelShape);
                if (fill) pair.getLeft().forEach(box -> drawBox(box.offset(vec3d), color, width, false, true, depth));
                pair.getRight().forEach(line -> drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth));
                return;
            }
            List<Line> lines = new ArrayList<>();
            voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> lines.add(new Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0)));
            SHAPE_OUTLINES.put(voxelShape, new Pair<>(voxelShape.getBoundingBoxes(), lines));
        }
    }

    public void drawBox(Box box, int color, float width) {
        drawBox(box, color, width, true, true, false);
    }

    public void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        drawBox(null, box, color, width, line, fill, depth);
    }

    public void drawBox(MatrixStack.Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        box = box.expand(1e-3);
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;
        if (fill) {
            int fillColor = ColorAssist.multAlpha(color, 0.1f);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
        }
        if (line) {
            drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
            drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
            drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
        }
    }

    public void vertexLine(MatrixStack matrices, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor) {
        vertexLine(matrices.peek(), buffer, start.toVector3f(), end.toVector3f(), startColor, endColor);
    }

    public void vertexLine(MatrixStack.Entry entry, VertexConsumer buffer, Vector3f start, Vector3f end, int startColor, int endColor) {
        if (entry == null) entry = lastWorldSpaceMatrix;
        Vector3f vec = getNormal(start, end);
        buffer.vertex(entry, start).color(startColor).normal(entry, vec);
        buffer.vertex(entry, end).color(endColor).normal(entry, vec);
    }

    public void vertexQuad(MatrixStack.Entry entry, VertexConsumer buffer, Vec3d vec1, Vec3d vec2, Vec3d vec3, Vec3d vec4, int color) {
        vertexQuad(entry, buffer, vec1.toVector3f(), vec2.toVector3f(), vec3.toVector3f(), vec4.toVector3f(), color);
    }

    public void vertexQuad(MatrixStack.Entry entry, VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color) {
        if (entry == null) entry = lastWorldSpaceMatrix;
        buffer.vertex(entry, vec1).color(color);
        buffer.vertex(entry, vec2).color(color);
        buffer.vertex(entry, vec3).color(color);
        buffer.vertex(entry, vec4).color(color);
    }

    public void quadTexture(MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
        buffer.vertex(entry, x, y + height, 0).texture(0, 0).color(color.x);
        buffer.vertex(entry, x + width, y + height, 0).texture(0, 1).color(color.y);
        buffer.vertex(entry, x + width, y, 0).texture(1, 1).color(color.w);
        buffer.vertex(entry, x, y, 0).texture(1, 0).color(color.z);
    }

    public Vector3f getNormal(Vector3f start, Vector3f end) {
        Vector3f normal = new Vector3f(start).sub(end);
        float sqrt = MathHelper.sqrt(normal.lengthSquared());
        return normal.div(sqrt);
    }

    public void drawCube(LivingEntity lastTarget, float anim, float red, String png) {
        float size = red - anim - 0.17F;
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = Calculate.interpolate(lastTarget).subtract(camera.getPos());
        boolean canSee = Objects.requireNonNull(mc.player).canSee(lastTarget);
        MatrixStack matrix = new MatrixStack();
        matrix.push();
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrix.translate(vec.x, vec.y + lastTarget.getBoundingBox().getLengthY() / 2, vec.z);
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(Calculate.interpolate(prevEspValue, espValue)));
        MatrixStack.Entry entry = matrix.peek().copy();
        int baseColor = (TargetESP.redOnHit.isValue() && red > 0) ? ColorAssist.red : TargetESP.getColor();
        int finalColor = ColorAssist.setAlpha(baseColor, (int)(ColorAssist.getAlpha(baseColor) * anim));
        Render3D.drawTexture(entry, Identifier.of("textures/capture" + png + ".png"), -size / 2, -size / 2, size, size, new Vector4i(finalColor, finalColor, finalColor, finalColor), false);
        matrix.pop();
    }
    private final Random random = new Random();
    private final List<Vec3d> particles = new ArrayList<>();
    public void drawCircle(MatrixStack matrix, LivingEntity lastTarget, float anim, float red) {
        double cs = Calculate.interpolate(circleStep - 0.17, circleStep);
        Vec3d target = Calculate.interpolate(lastTarget);
        boolean canSee = Objects.requireNonNull(mc.player).canSee(lastTarget);

        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        if (canSee) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int size = 64;
        for (int i = 0; i <= size; i++) {
            float width = lastTarget.getWidth();
            float height = lastTarget.getHeight();
            double yAnim = Calculate.absSinAnimation(cs) * height;
            double yAnim2 = Calculate.absSinAnimation(cs - 0.45) * height;
            Vec3d cosSin = Calculate.cosSin(i, size, width);
            Vec3d nextCosSin = Calculate.cosSin(i + 1, size, width);

            int baseColor = (TargetESP.redOnHit.isValue() && red > 0) ? ColorAssist.red : TargetESP.getColor();
            int color = ColorAssist.multRed(baseColor, 1 + red * 10);

            Vec3d start = target.add(cosSin.x, cosSin.y + yAnim, cosSin.z);
            Vec3d end   = target.add(cosSin.x, cosSin.y + yAnim2, cosSin.z);

            Render3D.vertexLine(matrix, buffer, start, end,
                    ColorAssist.multAlpha(color, 0.76F * anim),
                    ColorAssist.multAlpha(color, 0F));

            Render3D.drawLine(
                    target.add(cosSin.x, cosSin.y + yAnim, cosSin.z),
                    target.add(nextCosSin.x, nextCosSin.y + yAnim, nextCosSin.z),
                    ColorAssist.multAlpha(color, anim),
                    2, canSee
            );
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (canSee) {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }

    public static void renderCrystals(Entity entity, float anim, float red) {
        if (entity == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        AbstractTexture texture = mc.getTextureManager().getTexture(Identifier.of("minecraft", "textures/crystal.png"));
        texture.setFilter(true, false);
        RenderSystem.setShaderTexture(0, texture.getGlId());

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        MatrixStack stack = new MatrixStack();

        float time = (System.currentTimeMillis() % 90_000) / 90_000f;
        int color = (TargetESP.redOnHit.isValue() && red > 0) ? ColorAssist.red : TargetESP.getColor();
        int alphaColor = ColorAssist.setAlpha(color, (int) (ColorAssist.getAlpha(color) * anim));

        for (int i = 0; i < 20; i++) {
            float xOffset = (float) Math.sin(i + time * 360) * (entity.getWidth() + .1f);
            float yOffset = (float) Math.sin((i % (20 / 4f)) * entity.getHeight()) * entity.getHeight() / 3f;
            float zOffset = (float) Math.cos(i + time * 360) * (entity.getWidth() + .1f);

            float x = (float) MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), entity.prevX, entity.getX());
            float y = (float) MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), (float) entity.prevY, entity.getY());
            float z = (float) MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), entity.prevZ, entity.getZ());

            float halfSize = .4f;

            stack.push();
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            stack.translate(x - camera.getPos().x - xOffset, y + .2f + entity.getHeight() / 2f - camera.getPos().y - yOffset, z - camera.getPos().z - zOffset);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            Matrix4f matrix = stack.peek().getPositionMatrix();
            builder.vertex(matrix, -halfSize, halfSize, 0).texture(0, 0).color(alphaColor);
            builder.vertex(matrix, halfSize, halfSize, 0).texture(1, 0).color(alphaColor);
            builder.vertex(matrix, halfSize, -halfSize, 0).texture(1, 1).color(alphaColor);
            builder.vertex(matrix, -halfSize, -halfSize, 0).texture(0, 1).color(alphaColor);

            stack.pop();
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }
    public void drawGhosts(LivingEntity lastTarget, float anim, float red, float speed) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = Calculate.interpolate(lastTarget).subtract(camera.getPos());
        boolean canSee = mc.player.canSee(lastTarget);
        double iAge = Calculate.interpolate(mc.player.age - 1, mc.player.age);

        double radius = 0.6;
        float maxWidth = 0.5f;
        float minWidth = maxWidth * 0.3f;
        double distance = 0.1;
        int length = 70;
        float angleStep = 0.18f;
        int ghostCount = 4;

        int mainColor = ColorAssist.getClientColor();
        int secondColor = ColorAssist.getClientColor2();

        for (int orbit = 0; orbit < ghostCount; orbit++) {
            for (int i = length - 1; i >= 0; --i) {
                double angle = angleStep * (iAge * speed - i * distance);
                double s = Math.sin(angle) * radius;
                double c = Math.cos(angle) * radius;

                double xOffset = 0, yOffset = 0, zOffset = 0;
                double sinWave = Math.sin(Math.toRadians(iAge * 1.5 + orbit * 30)) * 0.3;

                switch (orbit) {
                    case 0:
                        xOffset = s;
                        yOffset = c ;
                        zOffset = -c;
                        break;
                    case 1:
                        xOffset = -s;
                        yOffset = s;
                        zOffset = -c;
                        break;
                    case 2:
                        xOffset = c;
                        yOffset = -s;
                        zOffset = -s;
                        break;
                    case 3:
                        xOffset = -c;
                        yOffset = -c;
                        zOffset = s;
                        break;
                }

                float progress = 1.0f - ((float) i / length);
                float currentWidth = minWidth + (maxWidth - minWidth) * progress * progress;

                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                matrices.translate(vec.x + xOffset, vec.y + lastTarget.getHeight() / 2 + yOffset, vec.z + zOffset);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                MatrixStack.Entry entry = matrices.peek().copy();

                int trailColor;
                if (progress < 0.3f) {
                    float whiteFactor = progress / 0.3f;
                    trailColor = ColorAssist.interpolateColor(TargetESP.getColor(), TargetESP.getColor(), whiteFactor);
                } else {
                    trailColor = TargetESP.getColor();
                }

                int finalColor = (TargetESP.redOnHit.isValue() && red > 0) ? ColorAssist.red : trailColor;

                float baseAlphaFactor = MathHelper.clamp(progress * anim, 0f, 1f);
                int finalAlpha = (int) (ColorAssist.getAlpha(finalColor) * baseAlphaFactor);
                int color = ColorAssist.getColor(ColorAssist.getRed(finalColor), ColorAssist.getGreen(finalColor), ColorAssist.getBlue(finalColor), finalAlpha);
                float scale = currentWidth * .8f * (0.8f + speed * 0.15f);

                Render3D.drawTexture(entry, bloom, -scale / 2, -scale / 2, scale, scale, new Vector4i(color), canSee);
            }
        }

        List<TargetESP.PositionEntry> positionHistory = TargetESP.targetPositionHistory;
        if (!positionHistory.isEmpty() && TargetESP.ghostMovement.isValue()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShaderTexture(0, bloom);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.disableDepthTest();
            
            int trailLength = positionHistory.size();
            int totalBlooms = trailLength * 3;
            
            for (int orbit = 0; orbit < ghostCount; orbit++) {
                for (int bloomIndex = totalBlooms - 1; bloomIndex >= 0; --bloomIndex) {
                    float t = (float) bloomIndex / (totalBlooms - 1);
                    int index = (int) (t * (trailLength - 1));
                    float localT = (t * (trailLength - 1)) - index;
                    
                    if (index >= positionHistory.size() - 1) {
                        index = positionHistory.size() - 1;
                        localT = 0;
                    }
                    
                    Vec3d pos1 = positionHistory.get(index).position;
                    Vec3d pos2 = index < positionHistory.size() - 1 ? positionHistory.get(index + 1).position : pos1;
                    Vec3d trailPos = new Vec3d(
                        MathHelper.lerp(localT, pos1.x, pos2.x),
                        MathHelper.lerp(localT, pos1.y, pos2.y),
                        MathHelper.lerp(localT, pos1.z, pos2.z)
                    );
                    Vec3d trailVec = trailPos.subtract(camera.getPos());
                    
                    double angle = angleStep * (iAge * speed - bloomIndex * distance / 3.0);
                    double s = Math.sin(angle) * radius;
                    double c = Math.cos(angle) * radius;

                    double xOffset = 0, yOffset = 0, zOffset = 0;

                    switch (orbit) {
                        case 0:
                            xOffset = s;
                            yOffset = c ;
                            zOffset = -c;
                            break;
                        case 1:
                            xOffset = -s;
                            yOffset = s;
                            zOffset = -c;
                            break;
                        case 2:
                            xOffset = c;
                            yOffset = -s;
                            zOffset = -s;
                            break;
                        case 3:
                            xOffset = -c;
                            yOffset = -c;
                            zOffset = s;
                            break;
                    }

                    float progress = 1.0f - ((float) bloomIndex / totalBlooms);
                    float currentWidth = minWidth + (maxWidth - minWidth) * progress * progress;

                    MatrixStack matrices = new MatrixStack();
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                    matrices.translate(trailVec.x + xOffset, trailVec.y + lastTarget.getHeight() / 2 + yOffset, trailVec.z + zOffset);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                    MatrixStack.Entry entry = matrices.peek().copy();

                    int trailColor;
                    if (progress < 0.3f) {
                        float whiteFactor = progress / 0.3f;
                        trailColor = ColorAssist.interpolateColor(TargetESP.getColor(), TargetESP.getColor(), whiteFactor);
                    } else {
                        trailColor = TargetESP.getColor();
                    }

                    int finalColor = (TargetESP.redOnHit.isValue() && red > 0) ? ColorAssist.red : trailColor;

                    float baseAlphaFactor = MathHelper.clamp(progress * anim, 0f, 1f);
                    int finalAlpha = (int) (ColorAssist.getAlpha(finalColor) * baseAlphaFactor);
                    int color = ColorAssist.getColor(ColorAssist.getRed(finalColor), ColorAssist.getGreen(finalColor), ColorAssist.getBlue(finalColor), finalAlpha);
                    float scale = currentWidth * .8f * (0.8f + speed * 0.15f);

                    Render3D.drawTexture(entry, bloom, -scale / 2, -scale / 2, scale, scale, new Vector4i(color), canSee);
                }
            }
            
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    public void drawGhostsWithTrail(LivingEntity lastTarget, float anim, float red, float speed) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d currentPos = Calculate.interpolate(lastTarget);
        Vec3d vec = currentPos.subtract(camera.getPos());
        boolean canSee = mc.player.canSee(lastTarget);
        double iAge = Calculate.interpolate(mc.player.age - 1, mc.player.age);

        List<TargetESP.PositionEntry> positionHistory = TargetESP.targetPositionHistory;
        if (positionHistory.isEmpty()) {
            return;
        }

        double radius = 0.5;
        float maxWidth = 0.5f;
        float minWidth = maxWidth * 0.3f;
        int ghostCount = 2;
        int trailLength = Math.min(40, positionHistory.size());
        int totalBlooms = trailLength * 3;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, bloom);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.disableDepthTest();

        for (int ghostIndex = 0; ghostIndex < ghostCount; ghostIndex++) {
            double angleOffset = (ghostIndex * Math.PI * 2) / ghostCount;
            double timeOffset = ghostIndex * 0.5;

            for (int bloomIndex = 0; bloomIndex < totalBlooms; bloomIndex++) {
                float t = (float) bloomIndex / (totalBlooms - 1);
                int index = (int) (t * (trailLength - 1));
                float localT = (t * (trailLength - 1)) - index;
                
                if (index >= positionHistory.size() - 1) {
                    index = positionHistory.size() - 1;
                    localT = 0;
                }
                
                Vec3d pos1 = positionHistory.get(index).position;
                Vec3d pos2 = index < positionHistory.size() - 1 ? positionHistory.get(index + 1).position : pos1;
                Vec3d trailPos = new Vec3d(
                    MathHelper.lerp(localT, pos1.x, pos2.x),
                    MathHelper.lerp(localT, pos1.y, pos2.y),
                    MathHelper.lerp(localT, pos1.z, pos2.z)
                );
                Vec3d trailVec = trailPos.subtract(camera.getPos());
                
                double angle = (iAge * speed * 0.15 + timeOffset + bloomIndex * 0.05) + angleOffset;
                double s = Math.sin(angle) * radius;
                double c = Math.cos(angle) * radius;
                
                double xOffset = s;
                double yOffset = c * 0.6;
                double zOffset = -c;

                float progress = 1.0f - ((float) bloomIndex / totalBlooms);
                float currentWidth = minWidth + (maxWidth - minWidth) * progress * progress;

                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                matrices.translate(trailVec.x + xOffset, trailVec.y + lastTarget.getHeight() / 2 + yOffset, trailVec.z + zOffset);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                MatrixStack.Entry entry = matrices.peek().copy();

                int trailColor;
                if (progress < 0.3f) {
                    float whiteFactor = progress / 0.3f;
                    trailColor = ColorAssist.interpolateColor(TargetESP.getColor(), TargetESP.getColor(), whiteFactor);
                } else {
                    trailColor = TargetESP.getColor();
                }

                int finalColor = (TargetESP.redOnHit.isValue() && red > 0) ? ColorAssist.red : trailColor;

                float baseAlphaFactor = MathHelper.clamp(progress * anim, 0f, 1f);
                int finalAlpha = (int) (ColorAssist.getAlpha(finalColor) * baseAlphaFactor);
                int color = ColorAssist.getColor(ColorAssist.getRed(finalColor), ColorAssist.getGreen(finalColor), ColorAssist.getBlue(finalColor), finalAlpha);
                float scale = currentWidth * .8f * (0.8f + speed * 0.15f);

                Render3D.drawTexture(entry, bloom, -scale / 2, -scale / 2, scale, scale, new Vector4i(color), canSee);
            }
        }

        if (trailLength > 1) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(3.0f);
            
            for (int ghostIndex = 0; ghostIndex < ghostCount; ghostIndex++) {
                double angleOffset = (ghostIndex * Math.PI * 2) / ghostCount;
                double timeOffset = ghostIndex * 0.5;
                
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
                
                for (int lineBloomIndex = 0; lineBloomIndex < totalBlooms; lineBloomIndex++) {
                    float t = (float) lineBloomIndex / (totalBlooms - 1);
                    int index = (int) (t * (trailLength - 1));
                    float localT = (t * (trailLength - 1)) - index;
                    
                    if (index >= positionHistory.size() - 1) {
                        index = positionHistory.size() - 1;
                        localT = 0;
                    }
                    
                    Vec3d pos1 = positionHistory.get(index).position;
                    Vec3d pos2 = index < positionHistory.size() - 1 ? positionHistory.get(index + 1).position : pos1;
                    Vec3d trailPos = new Vec3d(
                        MathHelper.lerp(localT, pos1.x, pos2.x),
                        MathHelper.lerp(localT, pos1.y, pos2.y),
                        MathHelper.lerp(localT, pos1.z, pos2.z)
                    );
                    Vec3d trailVec = trailPos.subtract(camera.getPos());
                    
                    double angle = (iAge * speed * 0.15 + timeOffset + lineBloomIndex * 0.05) + angleOffset;
                    double s = Math.sin(angle) * radius;
                    double c = Math.cos(angle) * radius;
                    
                    double xOffset = s;
                    double yOffset = c * 0.6;
                    double zOffset = -c;

                    float progress = 1.0f - ((float) lineBloomIndex / totalBlooms);
                    int trailColor = TargetESP.getColor();
                    int finalColor = (TargetESP.redOnHit.isValue() && red > 0) ? ColorAssist.red : trailColor;
                    float baseAlphaFactor = MathHelper.clamp(progress * anim * 0.7f, 0f, 1f);
                    int finalAlpha = (int) (ColorAssist.getAlpha(finalColor) * baseAlphaFactor);
                    int color = ColorAssist.getColor(ColorAssist.getRed(finalColor), ColorAssist.getGreen(finalColor), ColorAssist.getBlue(finalColor), finalAlpha);
                    
                    buffer.vertex((float) (trailVec.x + xOffset), (float) (trailVec.y + lastTarget.getHeight() / 2 + yOffset), (float) (trailVec.z + zOffset)).color(color);
                }
                
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }


    public static void renderNurik(Entity target,float anim,float red) {
        if (target == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        float tickDelta = Calculate.getTickDelta();
        double tPosX = MathHelper.lerp(tickDelta, target.prevX, target.getX()) - camera.getPos().x;
        double tPosY = MathHelper.lerp(tickDelta, target.prevY, target.getY()) - camera.getPos().y;
        double tPosZ = MathHelper.lerp(tickDelta, target.prevZ, target.getZ()) - camera.getPos().z;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, bloom);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        RenderSystem.disableDepthTest();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float xzRange = target.getWidth() * 1.34F;
        float yRange = target.getHeight();
        float sizeScale = 0.8F;
        float stretchFactor = 1.2F;
        int cornersCount = 155;
        int delayXZ = 1555;
        int delayY = (int)(1555 * 2);
        int step = 3;
        int wormTick = 0;
        int wormCD = 0;
        int wormCount = 0;
        long time = System.currentTimeMillis();

        for (int i = 0; i < cornersCount; i++) {
            float cornersPC = i / (float) cornersCount;

            float xzRotate = ((time + (long)(delayXZ * cornersPC)) % delayXZ) / (float) delayXZ * 360.0F;

            float yProgress = ((time + (long)(delayY * cornersPC)) % delayY) / (float) delayY;
            yProgress = yProgress > 0.5F ? 1.0F - yProgress : yProgress;
            yProgress *= 2.0F;
            yProgress = EasingList.QUAD_IN_OUT.ease(yProgress);

            double yawRad = Math.toRadians(MathHelper.wrapDegrees(cornersPC * 360.0F + xzRotate));

            float xPos = (float)(tPosX + Math.sin(yawRad) * xzRange);
            float yPos = (float)(tPosY + yRange * yProgress * stretchFactor);
            float zPos = (float)(tPosZ + Math.cos(yawRad) * xzRange);

            float size = (0.2F + 0.008F * wormTick) * sizeScale * stretchFactor;

            if (wormCD > 0) {
                wormCD -= step;
            } else {
                wormTick += step;
                if (wormTick > 50) {
                    wormCD = 105;
                    wormTick = 0;
                    wormCount++;
                } else {
                    MatrixStack matrices = new MatrixStack();
                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                    matrices.translate(xPos, yPos, zPos);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                    int color1 = ColorAssist.getClientColor(MathHelper.clamp(  anim, 0f, 1f));
                    int color3 = ColorAssist.getClientColor2(MathHelper.clamp( anim, 0f, 1f));

                    int trailColor;
                    if (cornersPC < 0.3f) {
                        float whiteFactor = cornersPC / 0.3f;
                        trailColor = ColorAssist.interpolateColor(TargetESP.getColor(), TargetESP.getColor(), whiteFactor);
                    } else {
                        trailColor = TargetESP.getColor();
                    }

                    int finalColor = (TargetESP.redOnHit.isValue() && red > 0) ? ColorAssist.red : trailColor;
                    float baseAlphaFactor = MathHelper.clamp( anim, 0f, 1f);
                    int finalAlpha = (int) (ColorAssist.getAlpha(finalColor) * baseAlphaFactor);
                    int color = ColorAssist.getColor(ColorAssist.getRed(finalColor), ColorAssist.getGreen(finalColor), ColorAssist.getBlue(finalColor), finalAlpha);

                    buffer.vertex(matrix, -size / 2,  size / 2, 0).texture(0f, 1f).color(color);
                    buffer.vertex(matrix,  size / 2,  size / 2, 0).texture(1f, 1f).color(color);
                    buffer.vertex(matrix,  size / 2, -size / 2, 0).texture(1f, 0f).color(color);
                    buffer.vertex(matrix, -size / 2, -size / 2, 0).texture(0f, 0f).color(color);
                }
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private float espValue = 1f, espSpeed = 1f, prevEspValue, circleStep;
    private boolean flipSpeed;

    public void updateTargetEsp() {
        prevEspValue = espValue;
        espValue += espSpeed;
        if (espSpeed > 25) flipSpeed = true;
        if (espSpeed < -25) flipSpeed = false;
        espSpeed = flipSpeed ? espSpeed - 0.5f : espSpeed + 0.5f;
        circleStep += 0.15f;
    }

    public void drawLine(MatrixStack.Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
        drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
    }

    public void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
        drawLine(null, start, end, color, color, width, depth);
    }

    public void drawLine(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Line line = new Line(entry, start, end, colorStart, colorEnd, width);
        if (depth) LINE_DEPTH.add(line);
        else LINE.add(line);
    }

    public static void drawCircleQuad(BufferBuilder buffer, MatrixStack.Entry entry, Vec3d center, double radius, int color, int segments) {
        buffer.vertex(entry, (float) center.x, (float) center.y, (float) center.z).color(color);
        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            buffer.vertex(entry, (float) (center.x + dx), (float) center.y, (float) (center.z + dz)).color(color);
        }
    }


    public void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        drawQuad(null, x, y, w, z, color, depth);
    }

    public void drawQuad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        Quad quad = new Quad(entry, x, y, w, z, color);
        if (depth) QUAD_DEPTH.add(quad);
        else QUAD.add(quad);
    }

    public void drawTexture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color, boolean depth) {
        Texture texture = new Texture(entry, id, x, y, width, height, color);
        if (depth) TEXTURE_DEPTH.add(texture);
        else TEXTURE.add(texture);
    }

    public record Texture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color) {}
    public record Line(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {}
    public record Quad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {}
}