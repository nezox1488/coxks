package fun.rich.display.hud;

import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import fun.rich.utils.client.managers.api.draggable.AbstractDraggable;
import fun.rich.common.animation.Animation;
import fun.rich.common.animation.Direction;
import fun.rich.common.animation.implement.Decelerate;
import fun.rich.utils.display.font.FontRenderer;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.client.Instance;
import fun.rich.utils.math.time.StopWatch;
import fun.rich.utils.client.chat.StringHelper;
import fun.rich.utils.display.geometry.Render2D;
import fun.rich.events.packet.PacketEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoolDowns extends AbstractDraggable {
    public static CoolDowns getInstance() {
        return Instance.getDraggable(CoolDowns.class);
    }

    public final List<CoolDown> list = new ArrayList<>();
    private long lastItemChange = 0;
    private int currentItemIndex = 0;
    private static final Item[] EXAMPLE_ITEMS = {
            Items.ENDER_EYE, Items.ENDER_PEARL, Items.SUGAR, Items.MACE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.TRIDENT, Items.CROSSBOW, Items.DRIED_KELP, Items.NETHERITE_SCRAP
    };

    public CoolDowns() {
        super("Cool Downs", 10, 40, 80, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
        list.stream().filter(c -> !Objects.requireNonNull(mc.player).getItemCooldownManager().isCoolingDown(c.item.getDefaultStack())).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastItemChange >= 1000) {
                currentItemIndex = (currentItemIndex + 1) % EXAMPLE_ITEMS.length;
                lastItemChange = currentTime;
            }
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        switch (e.getPacket()) {
            case CooldownUpdateS2CPacket c -> {
                Item item = Registries.ITEM.get(c.cooldownGroup());
                list.stream().filter(coolDown -> coolDown.item.equals(item)).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
                if (c.cooldown() != 0) {
                    list.add(new CoolDown(item, new StopWatch().setMs(-c.cooldown() * 50L), new Decelerate().setMs(150).setValue(1.0F)));
                }
            }
            case PlayerRespawnS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer fontCoolDown = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer icon = Fonts.getSize(21, Fonts.Type.ICONS);
        FontRenderer items = Fonts.getSize(12, Fonts.Type.DEFAULT);

        long activeCooldowns = list.stream().filter(c -> !c.anim.isFinished(Direction.BACKWARDS)).count();
        java.lang.String cooldownCountText = java.lang.String.valueOf(activeCooldowns);
        float textWidth = items.getStringWidth(cooldownCountText);
        float boxWidth = textWidth + 6;

        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 15.5F)
                .round(4f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ColorAssist.getRect(1.0f))
                .build());

        rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - boxWidth - 25, getY() + 3, boxWidth + 20, 10F)
                .round(2)
                .thickness(2)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(new Color(18, 19, 20, 55).getRGB())
                .build());

        items.drawString(matrix, "Active:", getX() + getWidth() - boxWidth - 22, getY() + 7, ColorAssist.getText());
        items.drawString(matrix, cooldownCountText, getX() + getWidth() - boxWidth - 2.5f, getY() + 7, new Color(255, 101, 57, 255).getRGB());

        rectangle.render(ShapeProperties.create(matrix, getX() + 18, getY() + 5, 0.5f, 6)
                .color(ColorAssist.getText(0.5F)).round(0F).build());
        rectangle.render(ShapeProperties.create(matrix, getX(), getY() + 16.5F, getWidth(), getHeight() - 17)
                .round(4f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ColorAssist.getRect(1.0f))
                .build());

        icon.drawString(matrix, "D", getX() + 4f, getY() + 5.5f, new Color(255, 101, 57, 255).getRGB());
        font.drawString(matrix, "CoolDowns", getX() + 22, getY() + 6.5f, ColorAssist.getText());

        float centerX = getX() + getWidth() / 2.0F;
        int offset = 23;
        int maxWidth = 110;

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            Item item = EXAMPLE_ITEMS[currentItemIndex];
            java.lang.String name = "Example CoolDowns";
            java.lang.String duration = "**:**";
            int textColor = ColorAssist.getText();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int orangeColor = new Color(255, 101, 57, 255).getRGB();
            float durationWidth = fontCoolDown.getStringWidth(duration);
            float durationBoxWidth = durationWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                Render2D.drawStack(matrix, item.getDefaultStack(), getX() + 3.5f, centerY - 3, false, 0.5F);
                rectangle.render(ShapeProperties.create(matrix, getX() + 14, centerY - 1, 0.5F, 6).color(ColorAssist.getOutline(1, 0.5F)).build());
                fontCoolDown.drawString(matrix, name, getX() + 18, centerY + 1, colorWithAlpha);
                rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - durationBoxWidth - 5, centerY - 2.5f, durationBoxWidth, 10F)
                        .round(2)
                        .thickness(2)
                        .outlineColor(new Color(33, 33, 33, 255).getRGB())
                        .color(new Color(18, 19, 20, 55).getRGB())
                        .build());
                fontCoolDown.drawString(matrix, duration, getX() + getWidth() - durationWidth - 8, centerY + 1, orangeColor);
            });
            int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        } else {
            for (CoolDown coolDown : list) {
                float animation = coolDown.anim.getOutput().floatValue();
                float centerY = getY() + offset;
                int time = -coolDown.time.elapsedTime() / 1000;
                java.lang.String name = coolDown.item.getDefaultStack().getName().getString();
                java.lang.String duration = StringHelper.getDuration(time);
                int textColor = ColorAssist.getText();
                int textAlpha = 255;
                int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
                int orangeColor = new Color(255, 101, 57, 255).getRGB();
                float durationWidth = fontCoolDown.getStringWidth(duration);
                float durationBoxWidth = durationWidth + 6;
                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    Render2D.drawStack(matrix, coolDown.item.getDefaultStack(), getX() + 3.5f, centerY - 3, false, 0.5F);
                    rectangle.render(ShapeProperties.create(matrix, getX() + 15, centerY - 1, 0.5F, 6).color(ColorAssist.getOutline(1, 0.5F)).build());
                    fontCoolDown.drawString(matrix, name, getX() + 18, centerY + 1, colorWithAlpha);
                    rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - durationBoxWidth - 5, centerY - 2.5f, durationBoxWidth, 10F)
                            .round(2)
                            .thickness(2)
                            .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(new Color(18, 19, 20, 55).getRGB())
                            .build());
                    fontCoolDown.drawString(matrix, duration, getX() + getWidth() - durationWidth - 8, centerY + 1, orangeColor);
                });
                int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
                maxWidth = Math.max(width, maxWidth);
                offset += (int) (11 * animation);
            }
        }
        setWidth(maxWidth + 10);
        setHeight(offset);
    }

    public record CoolDown(Item item, StopWatch time, Animation anim) {}
}