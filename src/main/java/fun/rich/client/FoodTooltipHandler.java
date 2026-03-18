package fun.rich.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class FoodTooltipHandler {

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            var food = stack.get(DataComponentTypes.FOOD);
            if (food == null) return;
            int nutrition = food.nutrition();
            float saturation = food.saturation();
            lines.add(Text.literal("Nutrition: " + nutrition).formatted(Formatting.GRAY));
            lines.add(Text.literal("Saturation: " + String.format("%.1f", saturation)).formatted(Formatting.GRAY));
        });
    }
}
