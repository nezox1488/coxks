package fun.rich.utils.display.widget;

import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.systemrender.builders.Builder;

import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public class SliderRender implements QuickImports {

    public static void renderSlider(DrawContext context, int x, int y, int width, int height, double value, boolean active, String text) {



        ShapeProperties animRect = ShapeProperties.create(
                        context.getMatrices(),
                        x + (width - width ) / 2f,
                        y + (height - height ) / 2f,
                        width ,
                        height
                )
                .round(5f)
                .color( new Color(18, 19, 20, 225).getRGB(),
                        new Color(0, 2, 5, 225).getRGB(),
                        new Color(0, 2, 5, 225).getRGB(),
                        new Color(18, 19, 20, 225).getRGB())
                .build();
        rectangle.render(animRect);

        ShapeProperties slider = ShapeProperties.create(context.getMatrices(), (float) (x + (value * (width - 8))), y, 8f, height)
                .round(3f)
                .color(new Color(24, 24, 24, active ? 255 : 0).getRGB())
                .build();
        rectangle.render(slider);

        if (text != null && !text.isEmpty()) {
            Fonts.getSize(18, Fonts.Type.DEFAULT)
                    .drawString(context.getMatrices(),
                            text,
                            x - Fonts.getSize(18, Fonts.Type.DEFAULT).getStringWidth(text) / 2f + width / 2f,
                            y + 7f,
                            Color.WHITE.getRGB());
        }
    }
}
