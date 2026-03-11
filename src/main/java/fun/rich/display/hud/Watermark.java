package fun.rich.display.hud;

import antidaunleak.api.UserProfile;
import com.google.common.base.Suppliers;
import fun.rich.utils.client.managers.api.draggable.AbstractDraggable;
import fun.rich.utils.display.atlasfont.msdf.MsdfFont;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.systemrender.builders.Builder;
import fun.rich.features.impl.render.Theme;
import fun.rich.utils.display.color.ColorAssist;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.Optional;
import java.util.function.Supplier;

import static fun.rich.utils.display.font.Fonts.Type.BOLD;


public class Watermark extends AbstractDraggable {

    private static final Supplier<MsdfFont> ICONS_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons").data("icons").build());
    private static final Supplier<MsdfFont> ICONS_FONT_1 = Suppliers.memoize(() -> MsdfFont.builder().atlas("clienticon1").data("clienticon1").build());

    private int fpsCount = 0;

    private static final float GAP_AFTER_FPS = 3f;

    public Watermark() {
        super("Watermark", 10, 10, 92, 16, true);
    }

    @Override
    public void tick() {
        fpsCount = mc.getCurrentFps();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();

        setHeight(10);

        final float logoSize = 8f;
        String logoIcon = "A ";
        var fr = Fonts.getSize(11, BOLD);
        String username = UserProfile.getInstance().profile("username");
        if (username == null) username = "";
        String serverIp = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Singleplayer";
        String ms = getPingMs() + " ms";
        String fps = fpsCount + " fps";

        float logoW = ICONS_FONT.get().getWidth(logoIcon, logoSize);
        float usernameW = fr.getStringWidth(username);
        float fpsW = fr.getStringWidth(fps);
        float serverW = fr.getStringWidth(serverIp);
        float msW = fr.getStringWidth(ms);
        float icon2W = ICONS_FONT_1.get().getWidth("D", 11);
        int totalW = (int) (logoW + 6 + usernameW + 4 + 5 + fpsW + 2 + GAP_AFTER_FPS + 4 + msW + 4 + 5 + serverW + 2 + icon2W + 6);
        setWidth(totalW);

        Theme theme = Theme.getInstance();
        int wBgColor = theme.watermarkBgColor.getColor();
        int wAlpha = (int) theme.watermarkBgAlpha.getValue();
        int fillColor = ColorAssist.setAlpha(wBgColor, wAlpha);
        int outlineColor = 0xFF2D2E41;
        float round = 6f;
        float thickness = 2f;

        if (theme.watermarkBlur.isValue()) {
            blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                    .round(round).quality(12).color(fillColor).build());
        }

        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(round)
                .softness(1)
                .thickness(thickness)
                .outlineColor(outlineColor)
                .color(fillColor)
                .build());

        Builder.text()
                .font(ICONS_FONT.get())
                .text(logoIcon)
                .size(logoSize)
                .color(new Color(225, 225, 255, 255).getRGB())
                .build()
                .render(matrix4f, getX() + 3, getY() + 0f);

        float cx = getX() + logoW + 6;

        fr.drawGradientString(matrix, username, cx, getY() + 4.5, 0xFF8187FF, 0xFF4D5199);
        cx += usernameW + 4;
        rectangle.render(ShapeProperties.create(matrix, cx, getY() + 6.5f, 0.5f, getHeight() - 8).round(0).color(ColorAssist.getText(0.5F)).build());
        cx += 5;

        fr.drawString(matrix, fps, cx, getY() + 4.5, -1);
        cx += fpsW + 2;
        Builder.text().font(ICONS_FONT_1.get()).text("w").size(9).color(new Color(225, 225, 255, 255).getRGB()).build().render(matrix4f, cx, getY() + 0.5f);
        cx += GAP_AFTER_FPS;
        rectangle.render(ShapeProperties.create(matrix, cx, getY() + 6.5f, 0.5f, getHeight() - 8).round(0).color(ColorAssist.getText(0.5F)).build());
        cx += 4;

        fr.drawString(matrix, ms, cx, getY() + 4.5, -1);
        cx += msW + 4;
        rectangle.render(ShapeProperties.create(matrix, cx, getY() + 6.5f, 0.5f, getHeight() - 8).round(0).color(ColorAssist.getText(0.5F)).build());
        cx += 5;

        fr.drawString(matrix, serverIp, cx, getY() + 4.5, -1);
        cx += serverW + 2;
        // Иконка после IP сервера — поднята выше (getY() - 0.5), чтобы была в одну линию с текстом
        Builder.text().font(ICONS_FONT_1.get()).text("D").size(8).color(new Color(225, 225, 255, 255).getRGB()).build().render(matrix4f, cx, getY() - 0.5f);
    }

    private int getPingMs() {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;
        return Optional.ofNullable(mc.getNetworkHandler().getPlayerListEntry(mc.player.getGameProfile().getId()))
                .map(e -> e.getLatency())
                .orElse(0);
    }
}
