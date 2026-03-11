package fun.rich.features.impl.render;
/**
 * @author Sitoku
 * @since 3/3/2026
 */
import fun.rich.events.container.HandledScreenEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.features.module.setting.implement.ColorSetting;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionHelper extends Module {

    private final ColorSetting cheapestColor = new ColorSetting("Цвет 1", "Самый дешевый").setColor(0x8000FF00);
    private final ColorSetting secondColor = new ColorSetting("Цвет 2", "Второй по цене").setColor(0x80FFFF00);
    private final ColorSetting thirdColor = new ColorSetting("Цвет 3", "Третий по цене").setColor(0x80FFA500);

    private final BooleanSetting showSecond = new BooleanSetting("Второй предмет", "Отображать второй выгодный предмет").setValue(true);
    private final BooleanSetting showThird = new BooleanSetting("Третий предмет", "Отображать третий выгодный предмет").setValue(true);

    private Slot cheapestSlot, secondSlot, thirdSlot;

    public AuctionHelper() {
        super("AuctionHelper", "AuctionHelper", ModuleCategory.RENDER);
        setup(cheapestColor, secondColor, thirdColor, showSecond, showThird);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            resetSlots();
            return;
        }

        String title = screen.getTitle().getString();
        if (!title.contains("Аукционы") && !title.contains("Аукцион") && !title.contains("Поиск:")) {
            resetSlots();
            return;
        }

        findCheapestSlots(screen.getScreenHandler().slots);
    }

    private void findCheapestSlots(List<Slot> slots) {
        cheapestSlot = null;
        secondSlot = null;
        thirdSlot = null;

        double firstPrice = Double.MAX_VALUE;
        double secondPrice = Double.MAX_VALUE;
        double thirdPrice = Double.MAX_VALUE;

        for (int i = 0; i < Math.min(slots.size(), 45); i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.GRAY_DYE) continue;

            long totalPrice = extractPriceFromStack(stack);
            if (totalPrice <= 0) continue;

            int count = stack.getCount();
            double pricePerItem = (double) totalPrice / (count > 0 ? count : 1);

            if (pricePerItem < firstPrice) {
                thirdPrice = secondPrice;
                thirdSlot = secondSlot;
                secondPrice = firstPrice;
                secondSlot = cheapestSlot;
                firstPrice = pricePerItem;
                cheapestSlot = slot;
            } else if (pricePerItem < secondPrice) {
                thirdPrice = secondPrice;
                thirdSlot = secondSlot;
                secondPrice = pricePerItem;
                secondSlot = slot;
            } else if (pricePerItem < thirdPrice) {
                thirdPrice = pricePerItem;
                thirdSlot = slot;
            }
        }
    }

    private static final Pattern PRICE_IN_LINE = Pattern.compile("(?:Цена|\\$|\\$\\s*Цена)[: ]*([\\d\\s,]+)");

    private long extractPriceFromStack(ItemStack stack) {
        try {
            LoreComponent lore = stack.getComponents().get(DataComponentTypes.LORE);
            if (lore != null) {
                for (Text line : lore.lines()) {
                    String raw = line.getString();
                    Matcher m = PRICE_IN_LINE.matcher(raw);
                    if (m.find()) {
                        String num = m.group(1).replaceAll("[\\s,]", "");
                        if (!num.isEmpty()) return Long.parseLong(num);
                    }
                    String noSp = raw.replace(" ", "").replace(",", "");
                    m = Pattern.compile("(?:Цена|\\$|\\$Цена)[: ]*(\\d+)").matcher(noSp);
                    if (m.find()) return Long.parseLong(m.group(1));
                }
            }

            String componentString = stack.getComponents().toString();
            int dollarIdx = componentString.indexOf("literal{ $");
            if (dollarIdx >= 0) {
                int start = dollarIdx + "literal{ $".length();
                int end = componentString.indexOf("}", start);
                if (end > start) {
                    String priceStr = componentString.substring(start, end).replaceAll("[\\s,]", "");
                    if (!priceStr.isEmpty()) return Long.parseLong(priceStr);
                }
            }

            String name = stack.getName().getString();
            Matcher nameMatcher = Pattern.compile("\\$\\s*([\\d\\s,]+)").matcher(name);
            if (nameMatcher.find()) {
                String num = nameMatcher.group(1).replaceAll("[\\s,]", "");
                if (!num.isEmpty()) return Long.parseLong(num);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private void resetSlots() {
        cheapestSlot = null;
        secondSlot = null;
        thirdSlot = null;
    }

    @EventHandler
    public void onRenderScreen(HandledScreenEvent e) {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;

        DrawContext context = e.getDrawContext();
        MatrixStack matrices = context.getMatrices();

        int guiX = (screen.width - e.getBackgroundWidth()) / 2;
        int guiY = (screen.height - e.getBackgroundHeight()) / 2;

        matrices.push();
        matrices.translate(guiX, guiY, 400);

        if (cheapestSlot != null) {
            drawHighlight(context, cheapestSlot, Calculate.applyOpacity(cheapestColor.getColor(), 100));
        }

        if (showSecond.isValue() && secondSlot != null) {
            drawHighlight(context, secondSlot, Calculate.applyOpacity(secondColor.getColor(), 100));
        }

        if (showThird.isValue() && thirdSlot != null) {
            drawHighlight(context, thirdSlot, Calculate.applyOpacity(thirdColor.getColor(), 100));
        }

        matrices.pop();
    }

    private void drawHighlight(DrawContext context, Slot slot, int color) {
        rectangle.render(ShapeProperties.create(
                context.getMatrices(),
                slot.x,
                slot.y,
                16,
                16
        ).color(color).build());
    }
}