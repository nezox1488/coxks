package fun.rich.display.screens.mainmenu;

import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.shape.ShapeProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class UpdateList implements QuickImports {

    public enum IconType { ADDED, REMOVED, CHANGED }

    public static class UpdateEntry {
        public final IconType iconType;
        public final String text;

        public UpdateEntry(IconType iconType, String text) {
            this.iconType = iconType;
            this.text = text;
        }
    }

    private final List<UpdateEntry> updates = new ArrayList<>();
    private float panelX;
    private float panelY;
    
    private final float headerWidth = 240;
    private final float headerHeight = 28;
    private final float listWidth = 240;
    private final float listHeight = 118;
    private float scroll = 0f;
    private float smoothedScroll = 0f;

    public UpdateList(float x, float y) {
        this.panelX = x;
        this.panelY = y;
        initUpdates();
    }

    private void initUpdates() {
        updates.add(new UpdateEntry(IconType.ADDED, "KTLeave: быстрый выход из PvP без лишних действий."));
        updates.add(new UpdateEntry(IconType.CHANGED, "Переработан Main Menu: список апдейтов стал аккуратнее и читаемее."));
        updates.add(new UpdateEntry(IconType.REMOVED, "XRay удалён: модуль больше не стабилен на серверах."));
    }

    public void updatePosition(float x, float y) {
        this.panelX = x;
        this.panelY = y;
    }

    public void render(DrawContext context, int alpha) {
        float gap = 6f;
        float headerX = panelX + (listWidth - headerWidth) / 2f;
        float headerY = panelY;
        float listX = panelX;
        float listY = panelY + headerHeight + gap;

        int blurAlpha = (int) (46 * (alpha / 255.0));
        int rectAlpha = (int) (34 * (alpha / 255.0));
        Color blurColor = new Color(20, 26, 34, blurAlpha);
        Color rectColor = new Color(22, 28, 38, rectAlpha);
        Color outlineColor = new Color(118, 196, 255, (int) (145 * (alpha / 255.0)));
        Color textColor = new Color(255, 255, 255, alpha);
        Color mutedText = new Color(182, 194, 210, alpha);

        blur.render(ShapeProperties.create(context.getMatrices(), headerX, headerY, headerWidth, headerHeight)
                .round(9).quality(96).color(blurColor.getRGB()).build());
        rectangle.render(ShapeProperties.create(context.getMatrices(), headerX, headerY, headerWidth, headerHeight)
                .round(9).thickness(1.2f).outlineColor(outlineColor.getRGB())
                .color(rectColor.getRGB(), rectColor.getRGB(), rectColor.getRGB(), rectColor.getRGB()).build());

        String title = "RoxsyClient 0.7";
        String subtitle = "Update List · 19.02.2026";

        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(context.getMatrices(), title,
                headerX + 10, headerY + 7, textColor.getRGB());
        Fonts.getSize(10, Fonts.Type.DEFAULT).drawString(context.getMatrices(), subtitle,
                headerX + headerWidth - Fonts.getSize(10, Fonts.Type.DEFAULT).getWidth(subtitle) - 10,
                headerY + 8, mutedText.getRGB());

        if (!updates.isEmpty()) {
            blur.render(ShapeProperties.create(context.getMatrices(), listX, listY, listWidth, listHeight)
                    .round(12).quality(96).color(blurColor.getRGB()).build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), listX, listY, listWidth, listHeight)
                    .round(12).thickness(1.3f).outlineColor(outlineColor.getRGB())
                    .color(rectColor.getRGB(), rectColor.getRGB(), rectColor.getRGB(), rectColor.getRGB()).build());

            rectangle.render(ShapeProperties.create(context.getMatrices(), listX + 8, listY + 8, listWidth - 16, 1)
                    .round(1).color(new Color(132, 210, 255, (int) (95 * (alpha / 255.0))).getRGB()).build());

            renderList(context, listX, listY, textColor, alpha);
        }

        if (updates.size() > 5) {
            renderScrollbar(context, listX, listY, alpha);
        }
    }

    private static final float ENTRY_SPACING = 30f;
    private static final float ICON_SIZE = 10f;
    private static final float TEXT_OFFSET = 20f;

    private void renderList(DrawContext context, float listX, float listY, Color textColor, int alpha) {
        float padding = 10;

        context.enableScissor((int) listX, (int) (listY + padding), (int) (listX + listWidth - padding), (int) (listY + listHeight - padding));
        smoothedScroll = MathHelper.lerp(0.15f, smoothedScroll, scroll);

        for (int i = 0; i < updates.size(); i++) {
            UpdateEntry entry = updates.get(i);
            float entryY = listY + padding + i * ENTRY_SPACING - smoothedScroll;

            if (entryY + 24 >= listY + padding && entryY <= listY + listHeight - padding) {
                int iconColor = getIconColor(entry.iconType, alpha);
                float iconX = listX + padding;
                float iconY = entryY + 6;

                rectangle.render(ShapeProperties.create(context.getMatrices(), iconX - 3, entryY + 3, listWidth - 20, 22)
                        .round(6)
                        .color(new Color(255, 255, 255, (int) (14 * (alpha / 255.0))).getRGB()).build());

                rectangle.render(ShapeProperties.create(context.getMatrices(), iconX, iconY, ICON_SIZE, ICON_SIZE)
                        .round(ICON_SIZE / 2f)
                        .color(iconColor, iconColor, iconColor, iconColor).build());

                Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(context.getMatrices(), wrapText(entry.text, 37),
                        iconX + TEXT_OFFSET, entryY + 7, ColorAssist.setAlpha(textColor.getRGB(), alpha));
            }
        }

        context.disableScissor();
    }

    private int getIconColor(IconType type, int alpha) {
        return switch (type) {
            case ADDED -> new Color(0, 255, 100, alpha).getRGB();
            case REMOVED -> new Color(255, 80, 80, alpha).getRGB();
            case CHANGED -> new Color(255, 200, 0, alpha).getRGB();
        };
    }

    private String wrapText(String text, int maxChars) {
        if (text.length() <= maxChars) return text;
        int space = text.lastIndexOf(' ', maxChars);
        if (space <= 0) return text.substring(0, Math.min(maxChars, text.length())) + "...";
        return text.substring(0, space) + "…";
    }

    private void renderScrollbar(DrawContext context, float listX, float listY, int alpha) {
        float contentH = updates.size() * ENTRY_SPACING;
        float maxScroll = Math.max(0, contentH - (listHeight - 24));
        scroll = MathHelper.clamp(scroll, 0, maxScroll);

        float sbW = 3;
        float sbX = listX + listWidth - sbW - 4;
        float sbY = listY + 12;
        float sbH = listHeight - 24;

        rectangle.render(ShapeProperties.create(context.getMatrices(), sbX, sbY, sbW, sbH)
                .round(1.5f).color(new Color(30, 30, 30, alpha / 2).getRGB()).build());

        float handleH = Math.max(24, sbH * (sbH / (contentH + sbH)));
        float ratio = maxScroll > 0 ? smoothedScroll / maxScroll : 0;
        float handleY = sbY + (sbH - handleH) * ratio;

        rectangle.render(ShapeProperties.create(context.getMatrices(), sbX, handleY, sbW, handleH)
                .round(1.5f).color(new Color(100, 180, 255, alpha / 2).getRGB()).build());
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        float listY = panelY + headerHeight + 6f;
        if (isIn(mouseX, mouseY, panelX, listY, listWidth, listHeight)) {
            float contentH = updates.size() * ENTRY_SPACING;
            float maxScroll = Math.max(0, contentH - (listHeight - 24));
            scroll -= (float) vertical * 24;
            scroll = MathHelper.clamp(scroll, 0, maxScroll);
            return true;
        }
        return false;
    }

    public void addEntry(IconType type, String text) {
        updates.add(new UpdateEntry(type, text));
    }

    private boolean isIn(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
