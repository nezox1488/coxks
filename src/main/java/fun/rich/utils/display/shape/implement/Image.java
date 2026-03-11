package fun.rich.utils.display.shape.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.shape.Shape;
import fun.rich.utils.display.shape.ShapeProperties;

@Setter
@Accessors(chain = true)
public class Image implements Shape, QuickImports {
    private String texture;

    /** Возвращает Identifier текстуры (после setTexture) */
    public Identifier getTexture() {
        return texture != null ? Identifier.of(texture) : null;
    }
    /** false = без поворота (для квадратных иконок), true = поворот 90° (backmenu и т.п.) */
    private boolean rotate = true;

    @Override
    public void render(ShapeProperties shape) {
        MatrixStack matrix = shape.getMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, Identifier.of(texture));

        float width = shape.getWidth();
        float height = shape.getHeight();
        float x = rotate ? shape.getX() + width : shape.getX();
        float y = shape.getY();

        if (rotate) {
            matrix.push();
            matrix.translate(x, y, 0.0F);
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
            matrix.translate(-x, -y, 0.0F);
        }

        drawEngine.quad(matrix.peek().getPositionMatrix(), x, y, height, width, shape.getColor().x);

        if (rotate) {
            matrix.pop();
        }

        RenderSystem.disableBlend();
    }
}
