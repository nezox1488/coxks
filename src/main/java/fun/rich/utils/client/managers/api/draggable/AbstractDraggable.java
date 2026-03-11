package fun.rich.utils.client.managers.api.draggable;

import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import fun.rich.common.animation.Animation;
import fun.rich.common.animation.Direction;
import fun.rich.common.animation.implement.Decelerate;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.interfaces.QuickLogger;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.geometry.Render2D;
import fun.rich.Rich;
import fun.rich.events.container.SetScreenEvent;
import fun.rich.events.packet.PacketEvent;
import fun.rich.features.impl.render.Hud;

@Setter
@Getter
public abstract class AbstractDraggable implements Draggable, QuickImports, QuickLogger {
    private String name;
    private int x, y, width, height;
    private boolean dragging, canDrag;
    private int dragX, dragY;

    public AbstractDraggable(String name, int x, int y, int width, int height, boolean canDrag) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.canDrag = canDrag;
    }

    /** Плавная анимация появления/скрытия элемента HUD (включение/выключение) */
    public final Animation scaleAnimation = new Decelerate().setValue(1).setMs(350);

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void tick() {}

    @Override
    public void packet(PacketEvent e) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!dragging) {
            dragX = 0;
            dragY = 0;
        }
        Hud hud = Hud.getInstance();
        float mouseDragX = mouseX + dragX;
        float mouseDragY = mouseY + dragY;
        int windowWidth = window.getScaledWidth();
        int windowHeight = window.getScaledHeight();
        int radius = 3;

        if (dragging) {
            int margin = 2;
            this.x = (int) Math.max(-margin, Math.min(mouseDragX, windowWidth - width + margin));
            this.y = (int) Math.max(-margin, Math.min(mouseDragY, windowHeight - height + margin));
        }

        for (AbstractDraggable drag : Rich.getInstance().getDraggableRepository().draggable()) {
            if (!drag.canDraw(hud, drag)) continue;
            if (!drag.canDrag) continue;
            if (drag == this) continue;
            int x1 = drag.x + drag.width + radius;
            int x2 = drag.x - width - radius;
            int y1 = drag.y + drag.height + radius;
            int y2 = drag.y - height - radius;
            int y3 = drag.y;

            if (Math.abs(x1 - mouseDragX) <= radius) {
                drawRect(x1 - 1.5F, 0, 1, windowHeight);
                this.x = x1;
            }
            if (Math.abs(x2 - mouseDragX) <= radius) {
                drawRect(x2 + width + 1, 0, 1, windowHeight);
                this.x = x2;
            }
            if (Math.abs(y1 - mouseDragY) <= radius) {
                drawRect(0, y1 - 1.5F, windowWidth, 1);
                this.y = y1;
            }
            if (Math.abs(y2 - mouseDragY) <= radius) {
                drawRect(0, y2 + height + 1, windowWidth, 1);
                this.y = y2;
            }
            if (Math.abs(y3 - mouseDragY) <= radius) {
                drawRect(0, y3 - 1.5F, windowWidth, 1);
                this.y = y3;
            }
        }

        if (Math.abs(x + (width - windowWidth) / 2) <= radius) {
            drawRect((float) windowWidth / 2 - 0.5F, 0, 1, windowHeight);
            this.x = (windowWidth - width) / 2;
        }
        if (Math.abs(y + (height - windowHeight) / 2) <= radius) {
            drawRect(0, (float) windowHeight / 2 - 0.5F, windowWidth, 1);
            this.y = (windowHeight - height) / 2;
        }
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        dragging = false;
        dragX = 0;
        dragY = 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0 && canDrag) {
            dragging = true;
            dragX = x - (int) mouseX;
            dragY = y - (int) mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        dragX = 0;
        dragY = 0;
        return true;
    }

    public abstract void drawDraggable(DrawContext context);

    /** Паддинг обводки — переопредели для кастомизации (Notifications: меньше по горизонтали) */
    protected int getOutlinePadX() { return 4; }
    protected int getOutlinePadY() { return 4; }
    /** Смещение обводки вниз — только для Watermark (+2px) */
    protected int getOutlineOffsetY() { return 0; }
    /** Уменьшить ширину обводки (Notifications) — сколько пикселей вычесть */
    protected int getOutlineWidthReduce() { return 0; }

    /** Обводка при перетаскивании — переливание между двумя цветами Theme */
    public void renderDragOutline(DrawContext context) {
        if (!dragging || !canDrag) return;
        var matrix = context.getMatrices();
        int padX = getOutlinePadX();
        int padY = getOutlinePadY();
        int reduce = getOutlineWidthReduce();
        float ox = x - padX + reduce / 2f;
        float oy = y - padY + getOutlineOffsetY();
        float ow = width + padX * 2f - reduce;
        float oh = height + padY * 2f;
        int c1 = ColorAssist.getClientColor();
        int c2 = ColorAssist.getClientColor2();
        int idx = (int) (System.currentTimeMillis() / 40) % 360;
        int ca = ColorAssist.fade(8, idx, c1, c2);
        int cb = ColorAssist.fade(8, idx + 90, c1, c2);
        int cc = ColorAssist.fade(8, idx + 180, c1, c2);
        int cd = ColorAssist.fade(8, idx + 270, c1, c2);
        float round = Math.min(8, Math.min(ow, oh) / 4f);
        rectangle.render(ShapeProperties.create(matrix, ox, oy, ow, oh)
                .round(round).thickness(2f).outlineColor(ca).color(ca, cb, cc, cd).build());
    }

    public void drawRect(float x, float y, float width, float height) {
        Render2D.drawQuad(x, y, width, height, ColorAssist.getText(0.5F));
    }

    public void stopAnimation() {
        scaleAnimation.setDirection(Direction.BACKWARDS);
    }

    public void startAnimation() {
        scaleAnimation.setDirection(Direction.FORWARDS);
    }

    public void validPosition() {
        int w = window.getScaledWidth();
        int h = window.getScaledHeight();
        int margin = 2;
        if (x + width > w + margin) x = w - width + margin;
        if (y + height > h + margin) y = h - height + margin;
        if (y < -margin) y = -margin;
        if (x < -margin) x = -margin;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isCloseAnimationFinished() {
        return scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    public boolean canDraw(Hud hud, AbstractDraggable draggable) {
        return hud.isState() && hud.interfaceSettings.isSelected(draggable.getName()) && visible();
    }
}