package fun.rich.display.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.rich.utils.client.managers.api.draggable.AbstractDraggable;
import fun.rich.utils.display.shape.ShapeProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import static net.minecraft.command.argument.ColorArgumentType.color;

public class ArmorHud extends AbstractDraggable {

    private static final String SHIELD_TEXTURE = "textures/features/shield.png";
    private static final float ROUND = 6f;
    private static final int BG_COLOR = 0xF2141724;
    private static final int OUTLINE_COLOR = 0xFF2D2E41;

    private DefaultedList<ItemStack> armor = DefaultedList.ofSize(4, ItemStack.EMPTY);

    private static final int BAR_HEIGHT = 18;
    private static final int RIGHT_PADDING = 6;
    private static final float ARMOR_SCALE = 0.7f;
    private static final int SLOT_STEP = 14;

    public ArmorHud() {
        super("Armor Hud", 220, 10, 14, BAR_HEIGHT, true);
    }

    @Override
    public boolean visible() {
        boolean hasArmor = armor.stream().anyMatch(s -> !s.isEmpty());
        return fun.rich.features.impl.render.Hud.getInstance().isState()
                && (hasArmor || mc.currentScreen instanceof ChatScreen);
    }

    @Override
    public void tick() {
        if (mc.player != null) {
            armor = mc.player.getInventory().armor;
        }
        super.tick();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack stack = context.getMatrices();

        int offset = 14;
        for (ItemStack itemStack : armor) {
            offset += SLOT_STEP;
        }
        setWidth(offset + RIGHT_PADDING);

        rectangle.render(ShapeProperties.create(stack, getX(), getY(), getWidth(), getHeight())
                .round(ROUND)
                .softness(1)
                .thickness(2)
                .outlineColor(OUTLINE_COLOR)
                .color(BG_COLOR)
                .build());
        rectangle.render(ShapeProperties.create(stack, getX() + 10.5f, getY() + (BAR_HEIGHT - 4) / 2f, 0.8f, 4)
                .color(OUTLINE_COLOR)
                .build());

        image.setTexture(SHIELD_TEXTURE).setRotate(true);
        image.render(ShapeProperties.create(stack, getX() + 3, getY() + (BAR_HEIGHT - 6) / 2f, 6, 6).build());

        float itemSize = 16 * ARMOR_SCALE;
        offset = 14;
        for (ItemStack itemStack : armor) {
            if (!itemStack.isEmpty()) {
                stack.push();
                stack.translate(getX() + offset, getY() + (BAR_HEIGHT - itemSize) / 2f, 0);
                stack.scale(ARMOR_SCALE, ARMOR_SCALE, 1f);
                RenderSystem.setShaderGlintAlpha(0);
                context.drawItem(itemStack, 0, 0);
                context.drawStackOverlay(mc.textRenderer, itemStack, 0, 0);
                RenderSystem.setShaderGlintAlpha(1);
                stack.pop();
            }
            offset += SLOT_STEP;
        }
    }
}
