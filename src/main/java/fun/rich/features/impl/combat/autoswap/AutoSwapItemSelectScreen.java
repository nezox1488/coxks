package fun.rich.features.impl.combat.autoswap;

import fun.rich.features.impl.combat.AutoSwap;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.shape.ShapeProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class AutoSwapItemSelectScreen extends Screen implements QuickImports {

    private final AutoSwap autoSwap;

    private final int wheelSlotIndex;

    private final int rows = 4;
    private final int cols = 9;
    private final int slotSize = 18;
    private final int slotPadding = 4;

    public AutoSwapItemSelectScreen(AutoSwap autoSwap, int wheelSlotIndex) {
        super(Text.literal("Выбор предмета"));
        this.autoSwap = autoSwap;
        this.wheelSlotIndex = wheelSlotIndex;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(new AutoSwapWheelScreen(autoSwap));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client == null || client.player == null) {
            super.render(ctx, mouseX, mouseY, delta);
            return;
        }

        int panelW = cols * (slotSize + slotPadding) + 16;
        int panelH = rows * (slotSize + slotPadding) + 40;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        rectangle.render(ShapeProperties.create(ctx.getMatrices(), x, y, panelW, panelH)
                .color(new Color(20, 20, 25, 230).getRGB())
                .round(8f)
                .build());

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("ЛКМ – выбрать предмет для слота " + wheelSlotIndex),
                x + 10, y + 8, 0xFFFFFFFF);

        int invX = x + 8;
        int invYStart = y + 24;

        var inv = client.player.getInventory();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                int sx = invX + col * (slotSize + slotPadding);
                int sy = invYStart + row * (slotSize + slotPadding);

                rectangle.render(ShapeProperties.create(ctx.getMatrices(), sx, sy, slotSize, slotSize)
                        .color(new Color(35, 35, 45, 200).getRGB())
                        .round(4f)
                        .build());

                ItemStack stack = inv.getStack(index);
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, sx + 1, sy + 1);
                }
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (client == null || client.player == null) return super.mouseClicked(mouseX, mouseY, button);
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);

        int panelW = cols * (slotSize + slotPadding) + 16;
        int panelH = rows * (slotSize + slotPadding) + 40;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        int invX = x + 8;
        int invYStart = y + 24;

        var inv = client.player.getInventory();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                int sx = invX + col * (slotSize + slotPadding);
                int sy = invYStart + row * (slotSize + slotPadding);

                if (mouseX >= sx && mouseX <= sx + slotSize &&
                        mouseY >= sy && mouseY <= sy + slotSize) {

                    ItemStack stack = inv.getStack(index);
                    if (!stack.isEmpty()) {
                        applyItem(stack);
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void applyItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        String itemName = stack.getName().getString();
        autoSwap.setWheelSlotItem(wheelSlotIndex, stack.getItem(), itemName);
        close();
    }

    @SuppressWarnings("unused")
    private String mapItemName(Item item) {
        if (item == Items.TOTEM_OF_UNDYING) return "Totem of Undying";
        if (item == Items.PLAYER_HEAD)      return "Player Head";
        if (item == Items.GOLDEN_APPLE)     return "Golden Apple";
        if (item == Items.SHIELD)           return "Shield";
        return null;
    }
}

