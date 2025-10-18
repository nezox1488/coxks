package fun.rich.features.impl.misc;

import fun.rich.events.keyboard.KeyEvent;
import fun.rich.events.player.InputEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.features.impl.movement.AutoSprint;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BindSetting;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.interactions.inv.InventoryResult;
import fun.rich.utils.interactions.inv.InventoryTask;
import fun.rich.utils.interactions.inv.InventoryToolkit;
import fun.rich.utils.math.script.Script;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.List;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElytraHelper extends Module {

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ замены").value("Default", "Legit").selected("Default");
    final BindSetting elytraSetting = new BindSetting("Замена элитр", "Меняет нагрудник на элитры");
    final BindSetting fireworkSetting = new BindSetting("Использовать фейерверк", "Меняет и использует фейерверки");
    final BooleanSetting startSetting = new BooleanSetting("Быстрый старт", "При замене на элитры автоматически взлетает и использует фейерверки").setValue(false);
    final BooleanSetting recast = new BooleanSetting("Авто взлет", "Автоматически начинает полет").setValue(false);
    final BooleanSetting autoFireworkSetting = new BooleanSetting("Авто фейерверк", "Автоматически использовать фейерверк каждые X мс").setValue(false);
    final SliderSettings fireworkDelay = new SliderSettings("Задержка использования в мс", "Задержка использования фейерверка в мс").setValue(500F).range(10F, 1500F).visible(() -> autoFireworkSetting.isValue());

    final Script script = new Script();

    enum ElytraPhase { READY, SLOWING_DOWN, WAITING_STOP, SWAP, SPEEDING_UP, FINISHED }
    ElytraPhase elytraPhase = ElytraPhase.READY;
    boolean slowingDown = false;
    float slowdownFactor = 1.0f;
    long swapStartTime = 0L;
    Slot targetSlot = null;
    boolean wasSprintingBefore = false;
    boolean playerFullyStopped = false;
    boolean wasForwardPressed, wasBackPressed, wasLeftPressed, wasRightPressed;
    boolean keysOverridden = false;

    enum FireworkPhase { READY, START, SWAP_TO_HOTBAR, WAITING_TO_USE, USE_FIREWORK, SWAP_BACK_TO_INV, FINISH }
    FireworkPhase fireworkPhase = FireworkPhase.READY;
    int fireworkSlot = -1;
    int savedHotbarSlot = -1;
    int originalHotbarSlot = -1;
    long useDelayStartTime = 0L;
    long lastAutoFireworkTime = 0L;

    public ElytraHelper() {
        super("ElytraHelper", "Elytra Helper", ModuleCategory.MISC);
        setup(modeSetting, elytraSetting, fireworkSetting, startSetting, recast, autoFireworkSetting, fireworkDelay);
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) && recast.isValue()) {
            if (mc.player.isOnGround()) e.setJumping(true);
            else if (!mc.player.isGliding()) PlayerInteractionHelper.startFallFlying();
        }

        if (slowingDown && mc.player != null) {
            if (elytraPhase == ElytraPhase.SLOWING_DOWN || elytraPhase == ElytraPhase.WAITING_STOP) {
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                    AutoSprint.tickStop = 10;
                }
            }
        }
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (!script.isFinished()) return;

        if (e.isKeyDown(elytraSetting.getKey())) {
            if (modeSetting.getSelected().equals("Default")) {
                executeDefaultSwap();
            } else if (modeSetting.getSelected().equals("Legit") && elytraPhase == ElytraPhase.READY) {
                startLegitSwap();
            }
        } else if (e.isKeyDown(fireworkSetting.getKey()) && mc.player.isGliding() && fireworkPhase == FireworkPhase.READY) {
            fireworkPhase = FireworkPhase.START;
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();

        if (elytraPhase != ElytraPhase.READY) {
            processLegitSwap();
        }

        if (fireworkPhase != FireworkPhase.READY) {
            if (modeSetting.getSelected().equals("Default")) {
                processDefaultFireworkUsage();
            } else {
                processLegitFireworkUsage();
            }
        }

        if (autoFireworkSetting.isValue() && mc.player != null && mc.player.isGliding()) {
            long now = System.currentTimeMillis();
            if (now - lastAutoFireworkTime >= (long) fireworkDelay.getValue()) {
                if (fireworkPhase == FireworkPhase.READY) {
                    if (modeSetting.getSelected().equals("Default")) {
                        InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.FIREWORK_ROCKET);
                        if (hotbar.found()) {
                            InventoryTask.swapAndUse(Items.FIREWORK_ROCKET);
                            lastAutoFireworkTime = now;
                        } else {
                            InventoryResult inv = InventoryToolkit.findItemInInventory(Items.FIREWORK_ROCKET);
                            if (inv.found()) {
                                int fireworkInvSlot = inv.slot();
                                int currentHotbarSlot = mc.player.getInventory().selectedSlot;
                                InventoryToolkit.clickSlot(fireworkInvSlot, currentHotbarSlot, SlotActionType.SWAP);
                                PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
                                InventoryToolkit.clickSlot(fireworkInvSlot, currentHotbarSlot, SlotActionType.SWAP);
                                lastAutoFireworkTime = now;
                            }
                        }
                    } else {
                        fireworkPhase = FireworkPhase.START;
                        lastAutoFireworkTime = now;
                    }
                }
            }
        }
    }

    private void processDefaultFireworkUsage() {
        if (mc.player == null) {
            resetFireworkState();
            return;
        }

        InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.FIREWORK_ROCKET);
        if (hotbar.found()) {
            InventoryTask.swapAndUse(Items.FIREWORK_ROCKET);
        } else {
            InventoryResult inv = InventoryToolkit.findItemInInventory(Items.FIREWORK_ROCKET);
            if (inv.found()) {
                int fireworkInvSlot = inv.slot();
                int currentHotbarSlot = mc.player.getInventory().selectedSlot;
                InventoryToolkit.clickSlot(fireworkInvSlot, currentHotbarSlot, SlotActionType.SWAP);
                PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
                InventoryToolkit.clickSlot(fireworkInvSlot, currentHotbarSlot, SlotActionType.SWAP);
            } else {
                ChatMessage.brandmessage("Нету фейерверков");
            }
        }
        resetFireworkState();
    }

    private void processLegitFireworkUsage() {
        if (mc.player == null || mc.currentScreen != null) {
            resetFireworkState();
            return;
        }

        switch (fireworkPhase) {
            case START -> {
                originalHotbarSlot = mc.player.getInventory().selectedSlot;
                InventoryResult hotbar = InventoryToolkit.findItemInHotBar(Items.FIREWORK_ROCKET);
                if (hotbar.found()) {
                    savedHotbarSlot = hotbar.slot();
                    InventoryToolkit.switchTo(savedHotbarSlot);
                    useDelayStartTime = System.currentTimeMillis();
                    fireworkPhase = FireworkPhase.WAITING_TO_USE;
                } else {
                    InventoryResult inv = InventoryToolkit.findItemInInventory(Items.FIREWORK_ROCKET);
                    if (inv.found()) {
                        fireworkSlot = inv.slot();
                        savedHotbarSlot = originalHotbarSlot;
                        fireworkPhase = FireworkPhase.SWAP_TO_HOTBAR;
                    } else {
                        ChatMessage.brandmessage("Нету фейерверков");
                        resetFireworkState();
                    }
                }
            }
            case SWAP_TO_HOTBAR -> {
                InventoryToolkit.clickSlot(fireworkSlot, savedHotbarSlot, SlotActionType.SWAP);
                useDelayStartTime = System.currentTimeMillis();
                fireworkPhase = FireworkPhase.WAITING_TO_USE;
            }
            case WAITING_TO_USE -> {
                if (System.currentTimeMillis() - useDelayStartTime > 20) {
                    fireworkPhase = FireworkPhase.USE_FIREWORK;
                }
            }
            case USE_FIREWORK -> {
                if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
                    PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
                }
                fireworkPhase = (fireworkSlot != -1) ? FireworkPhase.SWAP_BACK_TO_INV : FireworkPhase.FINISH;
            }
            case SWAP_BACK_TO_INV -> {
                InventoryToolkit.clickSlot(fireworkSlot, savedHotbarSlot, SlotActionType.SWAP);
                fireworkPhase = FireworkPhase.FINISH;
            }
            case FINISH -> resetFireworkState();
        }
    }

    private void resetFireworkState() {
        if (originalHotbarSlot != -1 && mc.player != null) {
            InventoryToolkit.switchTo(originalHotbarSlot);
        }
        fireworkPhase = FireworkPhase.READY;
        fireworkSlot = -1;
        savedHotbarSlot = -1;
        originalHotbarSlot = -1;
        useDelayStartTime = 0L;
    }

    private void executeDefaultSwap() {
        Slot slot = chestPlate();
        if (slot != null) {
            Slot fireWork = InventoryTask.getSlot(Items.FIREWORK_ROCKET);
            boolean elytra = slot.getStack().getItem().equals(Items.ELYTRA);
            InventoryTask.moveItem(slot, 6, true, true);

            if (startSetting.isValue() && fireWork != null && elytra) {
                script.cleanup().addTickStep(4, () -> {
                    if (mc.player.isOnGround()) mc.player.jump();
                }).addTickStep(3, () -> {
                    PlayerInteractionHelper.startFallFlying();
                    InventoryTask.swapAndUse(Items.FIREWORK_ROCKET);
                });
            }
        }
    }

    private void startLegitSwap() {
        targetSlot = chestPlate();
        if (targetSlot == null) return;

        if (mc.player != null) {
            wasSprintingBefore = mc.player.isSprinting();
        }

        wasForwardPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode());
        wasBackPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.backKey.getDefaultKey().getCode());
        wasLeftPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.leftKey.getDefaultKey().getCode());
        wasRightPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.rightKey.getDefaultKey().getCode());

        elytraPhase = ElytraPhase.SLOWING_DOWN;
        swapStartTime = System.currentTimeMillis();
        slowingDown = true;
        slowdownFactor = 1.0f;
        playerFullyStopped = false;
        keysOverridden = false;
    }

    private void processLegitSwap() {
        if (mc.player == null || mc.currentScreen != null) {
            resetLegitState();
            return;
        }

        long elapsed = System.currentTimeMillis() - swapStartTime;

        switch (elytraPhase) {
            case SLOWING_DOWN -> {
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                    AutoSprint.tickStop = 10;
                }
                if (!keysOverridden) {
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    keysOverridden = true;
                }
                if (elapsed > 1) {
                    elytraPhase = ElytraPhase.WAITING_STOP;
                }
            }
            case WAITING_STOP -> {
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                double velocityX = Math.abs(mc.player.getVelocity().x);
                double velocityZ = Math.abs(mc.player.getVelocity().z);
                double velocityY = Math.abs(mc.player.getVelocity().y);
                boolean isCompletelyStill = velocityX < 0.001 && velocityZ < 0.001 && (mc.player.isOnGround() || velocityY < 0.001);
                if (isCompletelyStill || elapsed > 5) {
                    playerFullyStopped = true;
                    elytraPhase = ElytraPhase.SWAP;
                }
            }
            case SWAP -> {
                if (playerFullyStopped) {
                    if (targetSlot != null) {
                        boolean elytra = targetSlot.getStack().getItem().equals(Items.ELYTRA);
                        InventoryTask.moveItem(targetSlot, 6, true, true);
                        if (startSetting.isValue() && elytra) {
                            Slot fireWork = InventoryTask.getSlot(Items.FIREWORK_ROCKET);
                            if (fireWork != null) {
                                script.cleanup().addTickStep(2, () -> {
                                    if (mc.player.isOnGround()) mc.player.jump();
                                }).addTickStep(1, () -> {
                                    PlayerInteractionHelper.startFallFlying();
                                    InventoryTask.swapAndUse(Items.FIREWORK_ROCKET);
                                });
                            }
                        }
                    }
                    elytraPhase = ElytraPhase.SPEEDING_UP;
                    slowdownFactor = 0.0f;
                    swapStartTime = System.currentTimeMillis();
                    slowingDown = false;
                    if (keysOverridden) {
                        restoreKeyStates();
                    }
                }
            }
            case SPEEDING_UP -> {
                long speedupElapsed = System.currentTimeMillis() - swapStartTime;
                float speedupProgress = Math.min(1.0f, speedupElapsed / 20.0f);
                slowdownFactor = speedupProgress;
                if (mc.player.input != null) {
                    boolean forward = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode());
                    boolean back = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.backKey.getDefaultKey().getCode());
                    boolean left = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.leftKey.getDefaultKey().getCode());
                    boolean right = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.rightKey.getDefaultKey().getCode());
                    float targetForward = 0, targetStrafe = 0;
                    if (forward) targetForward = 1.0f;
                    if (back) targetForward = -1.0f;
                    if (left) targetStrafe = 1.0f;
                    if (right) targetStrafe = -1.0f;
                    mc.player.input.movementForward = lerp(mc.player.input.movementForward, targetForward * slowdownFactor, 0.4f);
                    mc.player.input.movementSideways = lerp(mc.player.input.movementSideways, targetStrafe * slowdownFactor, 0.4f);
                    if (speedupProgress > 0.4f && forward && !mc.player.isSprinting()) {
                        mc.player.setSprinting(true);
                    }
                }
                if (speedupElapsed > 25) {
                    elytraPhase = ElytraPhase.FINISHED;
                }
            }
            case FINISHED -> resetLegitState();
        }
    }

    private void restoreKeyStates() {
        boolean currentForward = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode());
        boolean currentBack = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.backKey.getDefaultKey().getCode());
        boolean currentLeft = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.leftKey.getDefaultKey().getCode());
        boolean currentRight = InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.rightKey.getDefaultKey().getCode());

        mc.options.forwardKey.setPressed(wasForwardPressed && currentForward);
        mc.options.backKey.setPressed(wasBackPressed && currentBack);
        mc.options.leftKey.setPressed(wasLeftPressed && currentLeft);
        mc.options.rightKey.setPressed(wasRightPressed && currentRight);

        keysOverridden = false;
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    private void resetLegitState() {
        if (keysOverridden) {
            restoreKeyStates();
        }
        slowingDown = false;
        slowdownFactor = 1.0f;
        targetSlot = null;
        elytraPhase = ElytraPhase.READY;
        wasSprintingBefore = false;
        playerFullyStopped = false;
        wasForwardPressed = false;
        wasBackPressed = false;
        wasLeftPressed = false;
        wasRightPressed = false;
    }

    private Slot chestPlate() {
        if (Objects.requireNonNull(mc.player).getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)) {
            return InventoryTask.getSlot(List.of(
                    Items.NETHERITE_CHESTPLATE,
                    Items.DIAMOND_CHESTPLATE,
                    Items.IRON_CHESTPLATE,
                    Items.GOLDEN_CHESTPLATE,
                    Items.CHAINMAIL_CHESTPLATE,
                    Items.LEATHER_CHESTPLATE
            ));
        } else {
            return InventoryTask.getSlot(Items.ELYTRA);
        }
    }
}
