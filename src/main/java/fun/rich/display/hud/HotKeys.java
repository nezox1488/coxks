package fun.rich.display.hud;
// coded by sitoku and kifoz1337 \\
import fun.rich.Rich;
import fun.rich.features.impl.render.Hud;
import fun.rich.features.module.Module;
import fun.rich.utils.client.managers.api.draggable.AbstractDraggable;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.font.FontRenderer;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.client.chat.StringHelper;
import fun.rich.utils.math.Animation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HotKeys extends AbstractDraggable {
    private List<Module> keysList = new java.util.ArrayList<>();
    private final Map<String, Animation> lineAnimations = new HashMap<>();

    public HotKeys() {
        super("Hot Keys", 300, 40, 70, 18, true);
    }

    @Override
    public boolean visible() {
        return Hud.getInstance().interfaceSettings.isSelected("Hot Keys") &&
                (!keysList.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen));
    }

    @Override
    public void tick() {
        keysList = Rich.getInstance().getModuleProvider().getModules().stream()
                .filter(m -> m.isState() && m.getKey() != -1 && m.getKey() != 0)
                .collect(Collectors.toList());
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!visible()) return;

        MatrixStack matrix = context.getMatrices();
        FontRenderer fontTitle = Fonts.getSize(12, Fonts.Type.DEFAULT);
        FontRenderer fontModule = Fonts.getSize(11, Fonts.Type.DEFAULT);
        Hud hud = Hud.getInstance();

        float moduleY = getY() + 20;
        float heightCalc = 15;

        if (keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            heightCalc += 10;
        } else {
            heightCalc += keysList.size() * 10;
        }
        setHeight((int) heightCalc + 4);
        int alpha = (int) hud.opacityHotKeys.getValue();

        if (hud.khBlur.isValue()) {
            blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                    .round(8).quality((int) hud.khBlurAmount.getValue()).color(new Color(0, 0, 0, 100).getRGB()).build());
        }

        int c1 = ColorAssist.getClientColor();
        int c2 = ColorAssist.getClientColor2();
        int bgColor;
        var theme = fun.rich.features.impl.render.Theme.getInstance();
        if (theme != null) {
            bgColor = theme.hotkeysBgColor.getColor();
        } else {
            bgColor = new Color(18, 19, 20, 150).getRGB();
        }
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(8).thickness(0.5f).outlineColor(new Color(45, 45, 45, 255).getRGB())
                .color(ColorAssist.setAlpha(bgColor, alpha)).build());

        float sepW = getWidth() * 0.6f;
        float sepX = getX() + (getWidth() - sepW) / 2f;
        float sepY = getY() + 14;

        int sep1 = ColorAssist.fade(8, 200, c1, c2);
        int sep2 = ColorAssist.fade(8, 0, c1, c2);

        rectangle.render(ShapeProperties.create(matrix, sepX, sepY, sepW, 0.5f)
                .color(sep1, sep1, sep2, sep2).build());
        fontTitle.drawCenteredString(matrix, "Hotkeys", getX() + getWidth() / 2f, getY() + 6, ColorAssist.getText());
        if (keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            drawModuleLine(matrix, fontModule, "Example", "[NONE]", moduleY, 1f);
        } else {
            for (Module m : keysList) {
                Animation anim = lineAnimations.computeIfAbsent(m.getName(), k -> new Animation(0.5f));
                anim.setTarget(1.0);
                anim.update();
                float animVal = (float) anim.getValue();
                String bind = "[" + StringHelper.getBindName(m.getKey()) + "]";
                drawModuleLine(matrix, fontModule, m.getName(), bind, moduleY, animVal);
                moduleY += 8.5;
            }
        }
    }

    private void drawModuleLine(MatrixStack matrix, FontRenderer font, String name, String bind, float y, float anim) {
        float slide = (1f - anim) * 6f;
        float drawY = y + slide;
        int alpha = (int) (255 * anim);
        int nameColor = (255 << 24) | (Math.min(255, alpha) << 16) | (Math.min(255, alpha) << 8) | Math.min(255, alpha);
        int bindColor = (alpha << 24) | (180 << 16) | (180 << 8) | 180;
        float maxBindW = getWidth() - 50;
        if (font.getStringWidth(bind) > maxBindW) {
            bind = StringHelper.trimToWidth(bind, maxBindW - 8, font) + "..";
        }
        font.drawString(matrix, name, getX() + 6, drawY, nameColor);
        font.drawString(matrix, bind, getX() + getWidth() - font.getStringWidth(bind) - 6, drawY, bindColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}