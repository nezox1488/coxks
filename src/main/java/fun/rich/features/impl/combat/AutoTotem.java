package fun.rich.features.impl.combat;

import antidaunleak.api.annotation.Native;
import fun.rich.events.packet.PacketEvent;
import fun.rich.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.features.module.setting.implement.MultiSelectSetting;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.math.time.StopWatch;
import fun.rich.events.player.TickEvent;
import fun.rich.utils.math.script.Script;


import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTotem extends Module {
    SelectSetting swapMode = new SelectSetting("Обход", "Режим обхода")
            .value("Обычный", "Фантайм")
            .selected("Обычный");

    SliderSettings healthThreshold = new SliderSettings("Порог здоровья", "Минимальное здоровье для экипировки тотема")
            .setValue(4.5F).range(1F, 20F);

    BooleanSetting swapBack = new BooleanSetting("Вернуть предмет", "Возвращать предыдущий предмет обратно")
            .setValue(true);

    MultiSelectSetting modes = new MultiSelectSetting("Режимы", "Дополнительные условия для экипировки тотема")
            .value("Здоровие на элитре", "Падение", "Не брать если ешь", "Золотые серца", "Кристалы", "Обсидиан", "Булава", "Сайв таликов")
            .selected("Здоровие на элитре", "Падение", "Золотые серца", "Кристалы", "Обсидиан", "Булава", "Сайв таликов");

    SliderSettings elytraHealth = new SliderSettings("Здоровие на элитре", "Здоровье для экипировки тотема при полёте на элитре")
            .setValue(8.5F).range(1F, 20F)
            .visible(() -> modes.isSelected("Здоровие на элитре"));

    SliderSettings crystalDistance = new SliderSettings("Дистанция до криса", "Максимальная дистанция до кристалла для экипировки тотема")
            .setValue(4F).range(1F, 6F)
            .visible(() -> modes.isSelected("Кристалы"));

    SliderSettings obsidianDistance = new SliderSettings("Дистанция до обсы", "Максимальная дистанция до обсидиана для экипировки тотема")
            .setValue(4F).range(1F, 6F)
            .visible(() -> modes.isSelected("Обсидиан"));

    BooleanSetting noTakeIfBall = new BooleanSetting("Не брать если шар", " ")
            .setValue(true);

    StopWatch swapCooldown = new StopWatch();
    final Script script = new Script();
    int oldSlot = -1;
    ItemStack backItemStack = ItemStack.EMPTY;
    boolean totemIsUsed = false;
    long lastTotemUseTime = 0;
    boolean funtimeBlocking = false;

    public AutoTotem() {
        super("AutoTotem", "Auto Totem", ModuleCategory.COMBAT);
        setup(healthThreshold, swapBack, modes, elytraHealth, crystalDistance, obsidianDistance, noTakeIfBall);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

//        if (swapMode.isSelected("Фантайм")) {
//            handleFuntimeMode();
//            return;
//        }

        float health = mc.player.getHealth();
        float effectiveHealth = health;

        if (modes.isSelected("Золотые серца")) {
            effectiveHealth += mc.player.getAbsorptionAmount();
        }

        boolean shouldSwap = effectiveHealth <= healthThreshold.getValue() ||
                (totemIsUsed && getTotemCount() > 0 && System.currentTimeMillis() - lastTotemUseTime >= 1);

        if (modes.isSelected("Здоровие на элитре") && mc.player.isGliding() && health <= elytraHealth.getValue()) {
            shouldSwap = true;
        }

        if (modes.isSelected("Падение") && mc.player.fallDistance > 10) {
            shouldSwap = true;
        }

        if (modes.isSelected("Кристалы")) {
            double dist = getClosestCrystalDistance();
            if (dist <= crystalDistance.getValue()) {
                if (noTakeIfBall.isValue() && isHoldingSkull()) {
                    shouldSwap = effectiveHealth <= healthThreshold.getValue();
                } else {
                    shouldSwap = true;
                }
            }
        }

        if (modes.isSelected("Обсидиан")) {
            double dist = getClosestObsidianDistance();
            if (dist <= obsidianDistance.getValue()) {
                if (noTakeIfBall.isValue() && isHoldingSkull()) {
                    shouldSwap = effectiveHealth <= healthThreshold.getValue();
                } else {
                    shouldSwap = true;
                }
            }
        }

        if (modes.isSelected("Булава") && checkForMaceInEnemyHand()) {
            shouldSwap = true;
        }

        if (modes.isSelected("Не брать если ешь") && mc.player.isUsingItem() &&
                mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            shouldSwap = false;
        }

        ItemStack offhandStack = mc.player.getOffHandStack();
        boolean isEnchantedTotemInOffhand = offhandStack.getItem() == Items.TOTEM_OF_UNDYING &&
                EnchantmentHelper.hasEnchantments(offhandStack);

        if (shouldSwap && (!isTotemInOffhand() || isEnchantedTotemInOffhand)) {
            Slot totemSlot = findTotemSlot();
            if (totemSlot != null) {
                if (!offhandStack.isEmpty() && oldSlot == -1 && swapBack.isValue()) {
                    backItemStack = offhandStack.copy();
                    oldSlot = getSlotWithStack(backItemStack);
                }
                InventoryTask.swapHand(totemSlot, Hand.OFF_HAND, false, true);
                totemIsUsed = false;
            }
        } else if (!shouldSwap && swapBack.isValue() && isTotemInOffhand() && backItemStack != ItemStack.EMPTY) {
            int slot = getSlotWithStack(backItemStack);
            if (slot != -1) {
                Slot slotObj = InventoryTask.slots().filter(s -> s.id == slot).findFirst().orElse(null);
                if (slotObj != null) {
                    InventoryTask.swapHand(slotObj, Hand.OFF_HAND, false, true);
                    backItemStack = ItemStack.EMPTY;
                    oldSlot = -1;
                }
            }
        }
    }

    private void handleFuntimeMode() {
        if (funtimeBlocking) {
            script.update();
            if (script.isFinished()) {
                funtimeBlocking = false;
            }
            return;
        }

        float health = mc.player.getHealth();
        float effectiveHealth = health;

        if (modes.isSelected("Золотые серца")) {
            effectiveHealth += mc.player.getAbsorptionAmount();
        }

        boolean shouldSwap = effectiveHealth <= healthThreshold.getValue() ||
                (totemIsUsed && getTotemCount() > 0 && System.currentTimeMillis() - lastTotemUseTime >= 1);

        if (modes.isSelected("Здоровие на элитре") && mc.player.isGliding() && health <= elytraHealth.getValue()) {
            shouldSwap = true;
        }

        if (modes.isSelected("Падение") && mc.player.fallDistance > 10) {
            shouldSwap = true;
        }

        if (modes.isSelected("Кристалы")) {
            double dist = getClosestCrystalDistance();
            if (dist <= crystalDistance.getValue()) {
                if (noTakeIfBall.isValue() && isHoldingSkull()) {
                    shouldSwap = effectiveHealth <= healthThreshold.getValue();
                } else {
                    shouldSwap = true;
                }
            }
        }

        if (modes.isSelected("Обсидиан")) {
            double dist = getClosestObsidianDistance();
            if (dist <= obsidianDistance.getValue()) {
                if (noTakeIfBall.isValue() && isHoldingSkull()) {
                    shouldSwap = effectiveHealth <= healthThreshold.getValue();
                } else {
                    shouldSwap = true;
                }
            }
        }

        if (modes.isSelected("Булава") && checkForMaceInEnemyHand()) {
            shouldSwap = true;
        }

        if (modes.isSelected("Не брать если ешь") && mc.player.isUsingItem() &&
                mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            shouldSwap = false;
        }

        ItemStack offhandStack = mc.player.getOffHandStack();
        boolean isEnchantedTotemInOffhand = offhandStack.getItem() == Items.TOTEM_OF_UNDYING &&
                EnchantmentHelper.hasEnchantments(offhandStack);

        if (shouldSwap && (!isTotemInOffhand() || isEnchantedTotemInOffhand)) {
            Slot totemSlot = findTotemSlot();
            if (totemSlot != null) {
                funtimeBlocking = true;
                script.cleanup();
                script.addStep(1, () -> {
                    ItemStack currentOffhand = mc.player.getOffHandStack();
                    if (!currentOffhand.isEmpty() && oldSlot == -1 && swapBack.isValue()) {
                        backItemStack = currentOffhand.copy();
                        oldSlot = getSlotWithStack(backItemStack);
                    }
                    InventoryTask.swapHand(totemSlot, Hand.OFF_HAND, false, true);
                    totemIsUsed = false;
                });
                script.addStep(2000, () -> {
                });
            }
        } else if (!shouldSwap && swapBack.isValue() && isTotemInOffhand() && backItemStack != ItemStack.EMPTY) {
            int slot = getSlotWithStack(backItemStack);
            if (slot != -1) {
                Slot slotObj = InventoryTask.slots().filter(s -> s.id == slot).findFirst().orElse(null);
                if (slotObj != null) {
                    InventoryTask.swapHand(slotObj, Hand.OFF_HAND, false, true);
                    backItemStack = ItemStack.EMPTY;
                    oldSlot = -1;
                }
            }
        }
    }

//    public boolean isFuntimeBlocking() {
//        return funtimeBlocking && swapMode.isSelected("Фантайм");
//    }

    private boolean isHoldingSkull() {
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();

        return isSkull(mainHand) || isSkull(offHand);
    }

    private boolean isSkull(ItemStack stack) {
        if (stack.isEmpty()) return false;

        return stack.getItem() == Items.SKELETON_SKULL ||
                stack.getItem() == Items.WITHER_SKELETON_SKULL ||
                stack.getItem() == Items.ZOMBIE_HEAD ||
                stack.getItem() == Items.PLAYER_HEAD ||
                stack.getItem() == Items.CREEPER_HEAD ||
                stack.getItem() == Items.DRAGON_HEAD ||
                stack.getItem() == Items.PIGLIN_HEAD;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof EntityStatusS2CPacket statusPacket) {
            if (statusPacket.getStatus() == 35 && statusPacket.getEntity(mc.world) == mc.player) {
                totemIsUsed = true;
                lastTotemUseTime = System.currentTimeMillis();
            }
        }
    }

    private boolean checkForMaceInEnemyHand() {
        if (!modes.isSelected("Булава")) return false;
        Box box = mc.player.getBoundingBox().expand(30.0);
        List<AbstractClientPlayerEntity> players = mc.world.getEntitiesByClass(
                AbstractClientPlayerEntity.class, box, e -> true);
        for (AbstractClientPlayerEntity enemy : players) {
            if (enemy == mc.player) continue;
            ItemStack main = enemy.getMainHandStack();
            ItemStack off = enemy.getOffHandStack();
            if (main.getItem() == Items.MACE || off.getItem() == Items.MACE) {
                return true;
            }
        }
        return false;
    }

    private double getClosestCrystalDistance() {
        double minDist = Double.MAX_VALUE;
        Vec3d playerPos = mc.player.getPos();
        Box box = mc.player.getBoundingBox().expand(crystalDistance.getValue());
        List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true);
        for (EndCrystalEntity crystal : crystals) {
            double dist = playerPos.distanceTo(crystal.getPos());
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    private double getClosestObsidianDistance() {
        double minDist = Double.MAX_VALUE;
        BlockPos playerBlockPos = mc.player.getBlockPos();
        int dist = (int) Math.ceil(obsidianDistance.getValue());
        for (int x = -dist; x <= dist; x++) {
            for (int y = -dist; y <= dist; y++) {
                for (int z = -dist; z <= dist; z++) {
                    BlockPos pos = playerBlockPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN)) {
                        double d = MathHelper.sqrt((float) playerBlockPos.getSquaredDistance(pos));
                        if (d < minDist) {
                            minDist = d;
                        }
                    }
                }
            }
        }
        return minDist;
    }

    private boolean isTotemInOffhand() {
        ItemStack offhandStack = mc.player.getOffHandStack();
        return offhandStack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    private Slot findTotemSlot() {
        if (modes.isSelected("Сайв таликов")) {
            Slot nonEnchantedSlot = null;
            Slot enchantedSlot = null;
            for (Slot slot : InventoryTask.slots().toList()) {
                ItemStack stack = slot.getStack();
                if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    if (!EnchantmentHelper.hasEnchantments(stack)) {
                        nonEnchantedSlot = slot;
                    } else {
                        enchantedSlot = slot;
                    }
                }
            }
            if (nonEnchantedSlot != null) {
                return nonEnchantedSlot;
            }
            if (enchantedSlot != null) {
                return enchantedSlot;
            }
        } else {
            return InventoryTask.getSlot(Items.TOTEM_OF_UNDYING);
        }
        return null;
    }

    private int getTotemCount() {
        return (int) mc.player.getInventory().main.stream()
                .filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING)
                .count();
    }

    private int getSlotWithStack(ItemStack stack) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack invStack = mc.player.getInventory().getStack(i);
            if (ItemStack.areEqual(invStack, stack)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void deactivate() {
        oldSlot = -1;
        backItemStack = ItemStack.EMPTY;
        totemIsUsed = false;
        lastTotemUseTime = 0;
        swapCooldown.reset();
        funtimeBlocking = false;
        script.cleanup();
        super.deactivate();
    }
}