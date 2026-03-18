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

/**
 * UpdateList — панель обновлений на Main Menu в стиле размытой капли.
 * Позиция настраивается (panelX, panelY).
 * Иконки: зелёный круг = добавлено, красный = удалено, жёлтый = изменено.
 * Текстуры: конвертируй SVG в PNG и положи в assets/minecraft/textures/UpdateList/
 */
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
    
    /** Апдейт инфо — хедер с "RoxsyClient 0.7 | Update 19.02.2026" */
    private final float headerWidth = 160;
    private final float headerHeight = 22;
    private float headerOffsetX = 0f;  // Смещение хедера по X (можно сдвигать влево/вправо)
    
    /** АпдейтЛист — список обновлений с зелеными кругами и текстом */
    private final float listWidth = 240;
    private final float listHeight = 95;
    private float scroll = 0f;
    private float smoothedScroll = 0f;

    public UpdateList(float x, float y) {
        this.panelX = x;
        this.panelY = y;
        initUpdates();
    }

    private void initUpdates() {
        updates.add(new UpdateEntry(IconType.ADDED, "Добавлена функция KTLeave позволяет вам ливать в режиме пвп"));
        updates.add(new UpdateEntry(IconType.REMOVED, "Был удален модуль Xray так как больше не работает на серверах"));
    }

    public void updatePosition(float x, float y) {
        this.panelX = x;
        this.panelY = y;
    }

    public void render(DrawContext context, int alpha) {
        float gap = 6f;
        // Апдейт инфо — позиция с возможностью смещения
        float headerX = panelX + (listWidth - headerWidth) / 2f + headerOffsetX;
        float headerY = panelY;
        // АпдейтЛист — позиция списка обновлений
        float listX = panelX;
        float listY = panelY + headerHeight + gap;

        // Прозрачность как капля — фон мейнменю виден через панели
        int blurAlpha = (int) (30 * (alpha / 255.0));   // Blur: сильная прозрачность
        int rectAlpha = (int) (18 * (alpha / 255.0));   // Прямоугольник: очень тонкий
        Color blurColor = new Color(35, 40, 50, blurAlpha);
        Color rectColor = new Color(30, 35, 45, rectAlpha);
        Color outlineColor = new Color(100, 180, 255, (int) (90 * (alpha / 255.0)));
        Color textColor = new Color(255, 255, 255, alpha);

        // Апдейт инфо — хедер с названием и датой
        blur.render(ShapeProperties.create(context.getMatrices(), headerX, headerY, headerWidth, headerHeight)
                .round(10).quality(96).color(blurColor.getRGB()).build());
        rectangle.render(ShapeProperties.create(context.getMatrices(), headerX, headerY, headerWidth, headerHeight)
                .round(10).thickness(0).outlineColor(outlineColor.getRGB())
                .color(rectColor.getRGB(), rectColor.getRGB(), rectColor.getRGB(), rectColor.getRGB()).build());

        // Текст хедера (компактный)
        String header = "RoxsyClient 0.7 | Update 19.02.2026";
        Fonts.getSize(11, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), header,
                headerX + headerWidth / 2f, headerY + headerHeight / 2f - 3.5f, textColor.getRGB());

        // АпдейтЛист — список обновлений (рисуем только если есть элементы)
        if (!updates.isEmpty()) {
            blur.render(ShapeProperties.create(context.getMatrices(), listX, listY, listWidth, listHeight)
                    .round(11).quality(96).color(blurColor.getRGB()).build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), listX, listY, listWidth, listHeight)
                    .round(11).thickness(1.5f).outlineColor(outlineColor.getRGB())
                    .color(rectColor.getRGB(), rectColor.getRGB(), rectColor.getRGB(), rectColor.getRGB()).build());

            // Верхняя подсветка списка
            rectangle.render(ShapeProperties.create(context.getMatrices(), listX, listY, listWidth, 1)
                    .round(5).color(new Color(100, 180, 255, (int) (70 * (alpha / 255.0))).getRGB()).build());

            renderList(context, listX, listY, textColor, alpha);
        }

        if (updates.size() > 5) {
            renderScrollbar(context, listX, listY, alpha);
        }
    }

    private static final float ENTRY_SPACING = 26f;
    private static final float ICON_SIZE = 8f;
    private static final float TEXT_OFFSET = 18f;

    private void renderList(DrawContext context, float listX, float listY, Color textColor, int alpha) {
        float padding = 10;

        context.enableScissor((int) listX, (int) (listY + padding), (int) (listX + listWidth - padding), (int) (listY + listHeight - padding));
        smoothedScroll = MathHelper.lerp(0.15f, smoothedScroll, scroll);

        for (int i = 0; i < updates.size(); i++) {
            UpdateEntry entry = updates.get(i);
            float entryY = listY + padding + i * ENTRY_SPACING - smoothedScroll;

            if (entryY + 22 >= listY + padding && entryY <= listY + listHeight - padding) {
                int iconColor = getIconColor(entry.iconType, alpha);
                float iconX = listX + padding;
                float iconY = entryY + 6;

                // Кружок: рисуем через rectangle (круг через round = половина размера) — всегда виден
                rectangle.render(ShapeProperties.create(context.getMatrices(), iconX, iconY, ICON_SIZE, ICON_SIZE)
                        .round(ICON_SIZE / 2f)
                        .color(iconColor, iconColor, iconColor, iconColor).build());

                Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(context.getMatrices(), wrapText(entry.text, 32),
                        iconX + TEXT_OFFSET, entryY + 4, ColorAssist.setAlpha(textColor.getRGB(), alpha));
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
        return text.substring(0, space);
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
        if (isIn(mouseX, mouseY, panelX, panelY, listWidth, listHeight)) {
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
