package fun.rich.features.impl.combat;

import fun.rich.events.keyboard.KeyEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.events.render.DrawEvent;
import fun.rich.features.impl.combat.autoswap.AutoSwapItemSelectScreen;
import fun.rich.features.impl.combat.autoswap.AutoSwapWheelScreen;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BindSetting;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.features.module.setting.implement.MultiSelectSetting;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.interactions.inv.InventoryTask;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.math.script.Script;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import fun.rich.utils.client.logs.Logger;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
// fgdfg,e,wlllsad
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoSwap extends Module implements QuickImports {

    final BindSetting bind = new BindSetting("Кнопка свапа", "Кнопка для свапа");

    final SelectSetting mode = new SelectSetting("Режим", "Режим работы автосвапа")
            .value("Двойной", "Тройной")
            .selected("Двойной");

    final SelectSetting firstItem = new SelectSetting("Первый", "Первый предмет")
            .value("Щит", "Геплы", "Тотем", "Шар").selected("Тотем");
    final SelectSetting secondItem = new SelectSetting("Второй", "Второй предмет")
            .value("Щит", "Геплы", "Тотем", "Шар").selected("Тотем");
    final SelectSetting thirdItem = new SelectSetting("Третий", "Третий предмет")
            .value("Щит", "Геплы", "Тотем", "Шар").selected("Тотем")
            .visible(() -> mode.isSelected("Тройной"));

    final BooleanSetting ignoreRegularTotems = new BooleanSetting(
            "Игнорировать обычные тотемы",
            "Не свапать на обычные тотемы, только зачарованные"
    );

    final BooleanSetting autoDamage = new BooleanSetting(
            "Авто урон",
            "Автоматически свапает на предмет с максимальным уроном"
    );

    final MultiSelectSetting autoDamageConditions = new MultiSelectSetting(
            "Условия авто урона",
            "Когда включать авто урон"
    )
            .value("Таргет с малым HP", "У таргета нет меча")
            .selected("Таргет с малым HP")
            .visible(() -> autoDamage.isValue());

    final SliderSettings lowHpThreshold = new SliderSettings(
            "Порог HP",
            "Порог низкого HP таргета"
    )
            .setValue(10)
            .range(1F, 20F)
            .visible(() -> autoDamage.isValue() && autoDamageConditions.isSelected("Таргет с малым HP"));

    ItemStack savedOffhandItem = ItemStack.EMPTY;
    boolean autoDamageActive = false;

    Text lastSwapDisplayName = null;
    long lastSwapTimeMs = 0L;

    final Script script = new Script();

    static final long SWAP_TOAST_DURATION_MS = 1800L;
    static final long SWAP_FADE_MS = 250L;

    private static class WheelSlotItem {
        final Item item;
        final String itemName;

        WheelSlotItem(Item item, String itemName) {
            this.item = item;
            this.itemName = itemName;
        }
    }

    WheelSlotItem[] wheelSlots = new WheelSlotItem[3];

    public AutoSwap() {
        super("AutoSwap", "Auto Swap", ModuleCategory.COMBAT);
        setup(
                bind,
                mode,
                firstItem,
                secondItem,
                thirdItem,
                ignoreRegularTotems,
                autoDamage,
                autoDamageConditions,
                lowHpThreshold
        );
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();

        if (autoDamage.isValue()) {
            handleAutoDamage();
        }
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
        if (!e.isKeyDown(bind.getKey())) return;

        if (mode.isSelected("Тройной")) {
            if (mc.currentScreen == null) {
                mc.setScreen(new AutoSwapWheelScreen(this));
            }
        } else {
            handleDoubleSwap();
        }
    }

    private void handleDoubleSwap() {
        Item firstType = getItemByType(firstItem.getSelected());
        Item secondType = getItemByType(secondItem.getSelected());

        Slot first = firstType != Items.AIR
                ? InventoryTask.getSlot(
                firstType,
                Comparator.comparing(s -> s.getStack().hasEnchantments()),
                getSlotFilter(firstType)
        )
                : null;

        Slot second = secondType != Items.AIR
                ? InventoryTask.getSlot(
                secondType,
                Comparator.comparing(s -> s.getStack().hasEnchantments()),
                getSlotFilter(secondType)
        )
                : null;

        Slot validSlot = first != null && mc.player.getOffHandStack().getItem() != first.getStack().getItem()
                ? first
                : second;

        if (validSlot == null) return;

        ItemStack stackToSwap = validSlot.getStack();
        InventoryTask.swapHand(validSlot, Hand.OFF_HAND, false, true);
        script.cleanup().addTickStep(1, () -> triggerSwapToast(stackToSwap));
    }

    private void handleTripleSwap() {
        if (mc.player == null) return;

        Item offhandItem = mc.player.getOffHandStack().getItem();

        Item[] types = new Item[]{
                getItemByType(firstItem.getSelected()),
                getItemByType(secondItem.getSelected()),
                getItemByType(thirdItem.getSelected())
        };

        int startIndex = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] != Items.AIR && types[i] == offhandItem) {
                startIndex = (i + 1) % types.length;
                break;
            }
        }

        for (int i = 0; i < types.length; i++) {
            int idx = (startIndex + i) % types.length;
            Item type = types[idx];
            if (type == Items.AIR) continue;

            Slot slot = InventoryTask.getSlot(
                    type,
                    Comparator.comparing(s -> s.getStack().hasEnchantments()),
                    getSlotFilter(type)
            );

            if (slot == null) continue;
            if (slot.getStack().getItem() == offhandItem) continue;

            ItemStack stackToSwap = slot.getStack();
            InventoryTask.swapHand(slot, Hand.OFF_HAND, false, true);
            script.cleanup().addTickStep(1, () -> triggerSwapToast(stackToSwap));
            break;
        }
    }

    private void handleAutoDamage() {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = Aura.getInstance() != null ? Aura.getInstance().getTarget() : null;

        boolean shouldSwapToDamage = false;

        if (target != null && target.isAlive()) {
            boolean lowHpCondition = false;
            boolean noSwordCondition = false;

            if (autoDamageConditions.isSelected("Таргет с малым HP")) {
                boolean hpBelowThreshold = target.getHealth() <= lowHpThreshold.getValue();
                boolean hpLessThanMine = target.getHealth() < mc.player.getHealth();
                lowHpCondition = hpBelowThreshold && hpLessThanMine;
            }

            if (autoDamageConditions.isSelected("У таргета нет меча")) {
                if (target instanceof PlayerEntity player) {
                    ItemStack mainHand = player.getMainHandStack();
                    noSwordCondition = !(mainHand.getItem() instanceof net.minecraft.item.SwordItem);
                }
            }

            if (autoDamageConditions.getSelected().isEmpty()) {
                shouldSwapToDamage = true;
            } else {
                shouldSwapToDamage = lowHpCondition || noSwordCondition;
            }
        }

        if (shouldSwapToDamage && !autoDamageActive) {
            savedOffhandItem = mc.player.getOffHandStack().copy();

            ItemStack bestDamageItem = findBestDamageItem();
            if (!bestDamageItem.isEmpty()) {
                swapToItemStack(bestDamageItem);
                autoDamageActive = true;
            }
        } else if (!shouldSwapToDamage && autoDamageActive) {
            if (!savedOffhandItem.isEmpty()) {
                swapToItemStack(savedOffhandItem);
            }
            autoDamageActive = false;
            savedOffhandItem = ItemStack.EMPTY;
        }
    }

    private ItemStack findBestDamageItem() {
        if (mc.player == null) return ItemStack.EMPTY;

        List<ItemStack> damageItems = new ArrayList<>();

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() == Items.PLAYER_HEAD || stack.getItem() == Items.TOTEM_OF_UNDYING) {
                double damage = getAttackDamage(stack);
                if (damage > 0) {
                    damageItems.add(stack);
                }
            }
        }

        if (damageItems.isEmpty()) return ItemStack.EMPTY;

        return damageItems.stream()
                .max(Comparator.comparingDouble(this::getAttackDamage))
                .orElse(ItemStack.EMPTY);
    }

    private double getAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return 0;

        final double[] totalDamage = {0};

        modifiers.modifiers().forEach(entry -> {
            if (entry.attribute().value() == EntityAttributes.ATTACK_DAMAGE.value()) {
                totalDamage[0] += entry.modifier().value();
            }
        });

        return totalDamage[0];
    }

    private void swapToItemStack(ItemStack targetStack) {
        if (targetStack == null || targetStack.isEmpty()) return;

        Item item = targetStack.getItem();
        String itemName = targetStack.getName().getString();

        Slot foundSlot = null;
        for (Slot slot : InventoryTask.slots().filter(s -> s.id != 46 && s.id != 45).toList()) {
            ItemStack slotStack = slot.getStack();
            if (!slotStack.isEmpty() && slotStack.getItem() == item) {
                String slotName = slotStack.getName().getString();
                if (slotName.equals(itemName)) {
                    foundSlot = slot;
                    break;
                }
            }
        }

        if (foundSlot != null) {
            ItemStack stackToSwap = foundSlot.getStack();
            InventoryTask.swapHand(foundSlot, Hand.OFF_HAND, false, true);
            script.cleanup().addTickStep(1, () -> triggerSwapToast(stackToSwap));
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (lastSwapTimeMs == 0) return;
        if (mc == null || mc.getWindow() == null || mc.textRenderer == null) return;

        long elapsed = System.currentTimeMillis() - lastSwapTimeMs;
        if (elapsed > SWAP_TOAST_DURATION_MS) {
            lastSwapTimeMs = 0;
            lastSwapDisplayName = null;
            return;
        }

        boolean fadingIn = elapsed < SWAP_FADE_MS;
        boolean fadingOut = elapsed > (SWAP_TOAST_DURATION_MS - SWAP_FADE_MS);
        float anim = 1f;

        if (fadingIn) {
            anim = Math.min(1f, (float) elapsed / SWAP_FADE_MS);
        } else if (fadingOut) {
            anim = Math.max(0f, (float) (SWAP_TOAST_DURATION_MS - elapsed) / SWAP_FADE_MS);
        }

        if (anim <= 0.01f) return;

        DrawContext context = e.getDrawContext();
        if (context == null) return;

        MatrixStack matrix = context.getMatrices();

        final String prefixStr = "Свапнул на ";
        final int screenW = mc.getWindow().getScaledWidth();
        final int screenH = mc.getWindow().getScaledHeight();
        final float y = screenH - 80;

        final float prefixW = mc.textRenderer.getWidth(prefixStr);
        float nameW = 0f;
        if (lastSwapDisplayName != null) {
            nameW = mc.textRenderer.getWidth(lastSwapDisplayName);
        }
        final float totalW = prefixW + nameW;
        final float x = (screenW - totalW) / 2f;

        final float animFinal = anim;
        final int alpha = (int) (255 * animFinal);
        final int prefixColor = (alpha << 24) | 0xFFFFFF;
        final Text lastSwapDisplayNameFinal = lastSwapDisplayName;

        Calculate.setAlpha(animFinal, () -> {
            Calculate.scale(matrix, x + totalW / 2f, y + 6, animFinal, () -> {
                context.drawText(mc.textRenderer, prefixStr, (int) x, (int) y, prefixColor, false);
                if (lastSwapDisplayNameFinal != null) {
                    int nameColor = (alpha << 24) | 0xFFFFFF;
                    context.drawText(mc.textRenderer, lastSwapDisplayNameFinal, (int) (x + prefixW), (int) y, nameColor, false);
                }
            });
        });
    }

    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Щит" -> Items.SHIELD;
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Геплы" -> Items.GOLDEN_APPLE;
            case "Шар" -> Items.PLAYER_HEAD;
            // Дополнительные синонимы на всякий случай
            case "Гепл" -> Items.GOLDEN_APPLE;
            case "Сфера" -> Items.PLAYER_HEAD;
            default -> Items.AIR;
        };
    }

    private Predicate<Slot> getSlotFilter(Item item) {
        Predicate<Slot> base = s -> s.id != 45;
        if (item == Items.TOTEM_OF_UNDYING && ignoreRegularTotems.isValue()) {
            return base.and(s -> s.getStack().hasEnchantments());
        }
        return base;
    }

    private void triggerSwapToast(ItemStack swappedTo) {
        if (swappedTo == null || swappedTo.isEmpty()) return;
        lastSwapTimeMs = System.currentTimeMillis();
        lastSwapDisplayName = swappedTo.getName();
    }

    // ===================== WHEEL API =====================

    public void setWheelSlotItem(int index, Item item, String itemName) {
        if (index < 0 || index >= wheelSlots.length) return;
        wheelSlots[index] = new WheelSlotItem(item, itemName);
        Logger.info("[AutoSwap] setWheelSlotItem: index=" + index + ", item=" + (item != null ? item.toString() : "null") + ", name=" + itemName);
    }

    public void startSelectingItem(int wheelSlotIndex) {
        if (mc == null) return;
        mc.setScreen(new AutoSwapItemSelectScreen(this, wheelSlotIndex));
        Logger.info("[AutoSwap] Открыт выбор предмета для слота " + wheelSlotIndex);
    }

    public Item getWheelSlotItem(int index) {
        if (index >= 0 && index < wheelSlots.length && wheelSlots[index] != null) {
            return wheelSlots[index].item;
        }
        return null;
    }

    public ItemStack getWheelSlotStack(int index) {
        if (index < 0 || index >= wheelSlots.length) {
            Logger.warn("[AutoSwap] getWheelSlotStack: невалидный индекс " + index);
            return ItemStack.EMPTY;
        }
        WheelSlotItem slotItem = wheelSlots[index];
        if (slotItem == null || slotItem.item == null || slotItem.item == Items.AIR) {
            Logger.info("[AutoSwap] getWheelSlotStack: слот " + index + " пуст");
            return ItemStack.EMPTY;
        }
        if (mc == null || mc.player == null) {
            Logger.warn("[AutoSwap] getWheelSlotStack: mc или player null");
            return ItemStack.EMPTY;
        }
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == slotItem.item) {
                String stackName = stack.getName().getString();
                if (stackName.equals(slotItem.itemName)) {
                    Logger.info("[AutoSwap] getWheelSlotStack: найден стек для слота " + index + ", item=" + slotItem.item + ", name=" + slotItem.itemName + ", в инвентаре слот " + i);
                    return stack;
                }
            }
        }
        Logger.warn("[AutoSwap] getWheelSlotStack: предмет " + slotItem.item + " с названием '" + slotItem.itemName + "' не найден в инвентаре для слота " + index);
        return ItemStack.EMPTY;
    }

    public void startSwapToItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            Logger.warn("[AutoSwap] startSwapToItemStack: стек пуст");
            return;
        }

        Item item = stack.getItem();
        String itemName = stack.getName().getString();
        Logger.info("[AutoSwap] startSwapToItemStack: item=" + item + ", name=" + itemName);

        if (mc == null || mc.player == null || item == null || item == Items.AIR) {
            Logger.warn("[AutoSwap] startSwapToItemStack отменен");
            return;
        }

        Slot foundSlot = null;
        for (Slot slot : InventoryTask.slots().filter(s -> s.id != 46 && s.id != 45).toList()) {
            ItemStack slotStack = slot.getStack();
            if (!slotStack.isEmpty() && slotStack.getItem() == item) {
                String slotName = slotStack.getName().getString();
                if (slotName.equals(itemName)) {
                    foundSlot = slot;
                    break;
                }
            }
        }

        if (foundSlot != null) {
            ItemStack stackToSwap = foundSlot.getStack();
            Logger.info("[AutoSwap] Найден слот: id=" + foundSlot.id + ", stack=" + stackToSwap.getItem() + ", name=" + stackToSwap.getName().getString());
            InventoryTask.swapHand(foundSlot, Hand.OFF_HAND, false, true);
            Logger.info("[AutoSwap] Выполнен swapHand");
            script.cleanup().addTickStep(1, () -> triggerSwapToast(stackToSwap));
        } else {
            Logger.warn("[AutoSwap] Слот не найден для предмета: " + item + " с названием '" + itemName + "'");
        }
    }
}

