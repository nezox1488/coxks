package fun.rich.features.impl.combat;

import antidaunleak.api.annotation.Native;
import fun.rich.events.player.TickEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.interactions.inv.InventoryResult;
import fun.rich.utils.interactions.inv.InventoryToolkit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AutoTotem extends Module {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private final SelectSetting modeSetting = new SelectSetting("Режим", "Способ свопа")
            .value("Default", "Legit")
            .selected("Default");

    private final SliderSettings healthThreshold = new SliderSettings("Порог здоровья", "Минимальное здоровье для экипировки тотема")
            .setValue(4.5F).range(1F, 20F);

    private final SliderSettings elytraHealth = new SliderSettings("Здоровье на элитре", "Минимальное здоровье при полете")
            .setValue(8.5F).range(1F, 20F);

    private final SliderSettings crystalDistance = new SliderSettings("Дистанция до кристалла", "Макс дистанция до кристалла")
            .setValue(4F).range(1F, 6F);

    private final BooleanSetting fallCheck = new BooleanSetting("Падение", "Экипировать тотем при падении")
            .setValue(true);

    private final BooleanSetting saveTaliks = new BooleanSetting("Сейв таликов", "Использовать обычные тотемы без чар")
            .setValue(true);

    private long lastActionTime = 0L;
    private int savedSlot = -1;
    private int totemSlot = -1;
    private long actionStartTime = 0L;
    private boolean keysOverridden = false;
    private boolean wasForwardPressed, wasBackPressed, wasLeftPressed, wasRightPressed;
    private Phase phase = Phase.READY;

    private enum Phase { READY, SLOWING_DOWN, WAITING_STOP, PREPARE, AWAIT_SWITCH, EQUIP, SPEEDING_UP, FINISH }

    public AutoTotem() {
        super("AutoTotem", "Auto Totem", ModuleCategory.COMBAT);
        setup(modeSetting, healthThreshold, elytraHealth, crystalDistance, fallCheck, saveTaliks);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (MC.player == null || MC.world == null) {
            resetState();
            return;
        }

        float health = MC.player.getHealth();
        if (MC.player.isGliding() && health <= elytraHealth.getValue()) {
            tryEquipTotem();
        } else if (health <= healthThreshold.getValue()) {
            tryEquipTotem();
        } else if (fallCheck.isValue() && MC.player.fallDistance > 10) {
            tryEquipTotem();
        } else if (getClosestCrystalDistance() <= crystalDistance.getValue()) {
            tryEquipTotem();
        }

        if (phase != Phase.READY) execute();
    }

    private void tryEquipTotem() {
        if (phase != Phase.READY) return;
        if (isTotemInOffhand()) return;
        if (MC.currentScreen != null) return;

        savedSlot = MC.player.getInventory().selectedSlot;
        InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.TOTEM_OF_UNDYING);
        if (hotbar.found()) {
            totemSlot = hotbar.slot();
            startEquip();
            return;
        }

        InventoryResult inv = saveTaliks.isValue() ? findTotemWithSaveTalics() : InventoryToolkit.findItemInInventory(Items.TOTEM_OF_UNDYING);
        if (inv.found()) {
            totemSlot = inv.slot();
            if (modeSetting.getSelected().equals("Legit")) {
                wasForwardPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.forwardKey.getDefaultKey().getCode());
                wasBackPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.backKey.getDefaultKey().getCode());
                wasLeftPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.leftKey.getDefaultKey().getCode());
                wasRightPressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.rightKey.getDefaultKey().getCode());
                phase = Phase.SLOWING_DOWN;
                actionStartTime = System.currentTimeMillis();
            } else {
                phase = Phase.PREPARE;
            }
        } else {
            ChatMessage.brandmessage("Нет тотема");
            resetState();
        }
    }

    private void startEquip() {
        phase = Phase.PREPARE;
    }

    private void execute() {
        if (MC.player == null || MC.currentScreen != null) {
            resetState();
            return;
        }

        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (phase) {
            case SLOWING_DOWN -> {
                MC.player.input.movementForward = 0;
                MC.player.input.movementSideways = 0;
                if (!keysOverridden) {
                    MC.options.forwardKey.setPressed(false);
                    MC.options.backKey.setPressed(false);
                    MC.options.leftKey.setPressed(false);
                    MC.options.rightKey.setPressed(false);
                    keysOverridden = true;
                }
                if (elapsed > 1) phase = Phase.WAITING_STOP;
            }
            case WAITING_STOP -> {
                MC.player.input.movementForward = 0;
                MC.player.input.movementSideways = 0;
                double vx = Math.abs(MC.player.getVelocity().x);
                double vz = Math.abs(MC.player.getVelocity().z);
                if ((vx < 0.001 && vz < 0.001) || elapsed > 15) phase = Phase.PREPARE;
            }
            case PREPARE -> {
                if (totemSlot < 0) {
                    resetState();
                    return;
                }

                int slotIndex = totemSlot;
                if (slotIndex >= 0 && slotIndex <= 8) slotIndex += 36;

                if (MC.interactionManager != null && MC.player != null && MC.player.playerScreenHandler != null) {
                    MC.interactionManager.clickSlot(
                            MC.player.playerScreenHandler.syncId,
                            slotIndex,
                            40,
                            SlotActionType.SWAP,
                            MC.player
                    );
                }

                phase = Phase.AWAIT_SWITCH;
            }

            case AWAIT_SWITCH -> {
                if (isTotemInOffhand()) phase = Phase.EQUIP;
            }
            case EQUIP -> {
                InventoryToolkit.switchTo(savedSlot);
                if (modeSetting.getSelected().equals("Legit")) {
                    restoreKeyStates();
                    actionStartTime = System.currentTimeMillis();
                    phase = Phase.SPEEDING_UP;
                } else phase = Phase.FINISH;
            }
            case SPEEDING_UP -> {
                long speedupElapsed = System.currentTimeMillis() - actionStartTime;
                float progress = Math.min(1.0f, speedupElapsed / 20.0f);
                if (MC.player.input != null) {
                    boolean forward = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.forwardKey.getDefaultKey().getCode());
                    float targetForward = forward ? 1.0f : 0;
                    MC.player.input.movementForward = lerp(MC.player.input.movementForward, targetForward * progress, 0.4f);
                }
                if (speedupElapsed > 25) phase = Phase.FINISH;
            }
            case FINISH -> resetState();
        }
    }

    private InventoryResult findTotemWithSaveTalics() {
        InventoryResult nonEnchanted = InventoryToolkit.findInInventory(i -> i.getItem() == Items.TOTEM_OF_UNDYING && !i.hasEnchantments());
        if (nonEnchanted.found()) return nonEnchanted;
        return InventoryToolkit.findItemInInventory(Items.TOTEM_OF_UNDYING);
    }

    private double getClosestCrystalDistance() {
        double minDist = Double.MAX_VALUE;
        if (MC.player == null || MC.world == null) return minDist;
        Box box = MC.player.getBoundingBox().expand(crystalDistance.getValue());
        List<EndCrystalEntity> crystals = MC.world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true);
        for (EndCrystalEntity crystal : crystals) {
            double dist = MC.player.getPos().distanceTo(crystal.getPos());
            if (dist < minDist) minDist = dist;
        }
        return minDist;
    }

    private boolean isTotemInOffhand() {
        ItemStack stack = MC.player.getOffHandStack();
        return stack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    private void restoreKeyStates() {
        if (!keysOverridden) return;
        boolean currentForward = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.forwardKey.getDefaultKey().getCode());
        boolean currentBack = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.backKey.getDefaultKey().getCode());
        boolean currentLeft = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.leftKey.getDefaultKey().getCode());
        boolean currentRight = InputUtil.isKeyPressed(MC.getWindow().getHandle(), MC.options.rightKey.getDefaultKey().getCode());
        MC.options.forwardKey.setPressed(wasForwardPressed && currentForward);
        MC.options.backKey.setPressed(wasBackPressed && currentBack);
        MC.options.leftKey.setPressed(wasLeftPressed && currentLeft);
        MC.options.rightKey.setPressed(wasRightPressed && currentRight);
        keysOverridden = false;
    }

    private void resetState() {
        if (keysOverridden) restoreKeyStates();
        totemSlot = -1;
        savedSlot = -1;
        actionStartTime = 0L;
        phase = Phase.READY;
    }

    @Override
    public void deactivate() {
        resetState();
        super.deactivate();
    }
}
