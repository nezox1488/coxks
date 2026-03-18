package fun.rich.features.impl.misc;

import antidaunleak.api.annotation.Native;
import fun.rich.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.*;
import fun.rich.utils.math.time.StopWatch;
import fun.rich.events.player.TickEvent;
import fun.rich.events.render.WorldRenderEvent;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.geometry.Render3D;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChestStealer extends Module {
    StopWatch stopWatch = new StopWatch();
    StopWatch barrelScanTimer = new StopWatch();
    StopWatch openCooldown = new StopWatch();

    SelectSetting modeSetting = new SelectSetting("Тип", "Выбирает тип стила")
            .value("FunTime", "WhiteList", "Default", "CopperDungeon").selected("FunTime");

    SliderSettings delaySetting = new SliderSettings("Задержка", "Задержка между кликами по слоту")
            .setValue(100).range(0, 1000).visible(() -> modeSetting.isSelected("WhiteList") || modeSetting.isSelected("Default"));

    MultiSelectSetting itemSettings = new MultiSelectSetting("Предметы", "Выберите предметы, которые вор будет подбирать")
            .value("Tripwire Hook",
                    "Ender Eye",
                    "Snowball",
                    "Splash Potion",
                    "Netherite Scrap",
                    "Enchanted Golden Apple",
                    "Golden Apple",
                    "TNT",
                    "Player Head",
                    "Totem of Undying",
                    "Iron Ingot",
                    "Experience Bottle")
            .visible(() -> modeSetting.isSelected("WhiteList") || modeSetting.isSelected("CopperDungeon"));

    SliderSettings rangeSetting = new SliderSettings("Радиус", "Радиус поиска бочек")
            .setValue(3).range(1, 5).visible(() -> modeSetting.isSelected("CopperDungeon"));

    SliderSettings openDelaySetting = new SliderSettings("Задержка открытия", "Задержка между открытиями бочек")
            .setValue(50).range(0, 500).visible(() -> modeSetting.isSelected("CopperDungeon"));

    BooleanSetting tracerSetting = new BooleanSetting("Tracer", "Показывать трассер к бочкам")
            .setValue(true).visible(() -> modeSetting.isSelected("CopperDungeon"));

    ColorSetting tracerColor = new ColorSetting("Цвет трассера", "Цвет трассера к бочкам")
            .value(ColorAssist.getColor(255, 0, 0, 255)).visible(() -> modeSetting.isSelected("CopperDungeon") && tracerSetting.isValue());

    SliderSettings tracerTimeSetting = new SliderSettings("Время трассера", "Показывать трассер если до лута меньше (сек)")
            .setValue(30).range(1, 120).visible(() -> modeSetting.isSelected("CopperDungeon") && tracerSetting.isValue());

    BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Открывать бочки через стены")
            .setValue(false).visible(() -> modeSetting.isSelected("CopperDungeon"));

    Map<BlockPos, Long> barrelTimers = new HashMap<>();
    Map<BlockPos, TextDisplayEntity> barrelTextEntities = new HashMap<>();
    List<BlockPos> barrelsToLoot = new ArrayList<>();
    BlockPos lastOpenedBarrel = null;

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})");

    public ChestStealer() {
        super("ChestStealer", "Chest Stealer", ModuleCategory.MISC);
        setup(modeSetting, delaySetting, itemSettings, rangeSetting, openDelaySetting,
                tracerSetting, tracerColor, tracerTimeSetting, throughWalls);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        switch (modeSetting.getSelected()) {
            case "FunTime" -> {
                if (mc.currentScreen instanceof GenericContainerScreen sh &&
                        sh.getTitle().getString().toLowerCase().contains("мистический") &&
                        !mc.player.getItemCooldownManager().isCoolingDown(Items.GUNPOWDER.getDefaultStack())) {

                    sh.getScreenHandler().slots.stream()
                            .filter(s -> s.hasStack() && !s.inventory.equals(mc.player.getInventory()) && stopWatch.every(150))
                            .forEach(s -> InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true));
                }
            }

            case "CopperDungeon" -> {
                handleCopperDungeon();
            }

            case "WhiteList", "Default" -> {
                if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler sh) {
                    sh.slots.forEach(s -> {
                        if (s.hasStack() && !s.inventory.equals(mc.player.getInventory()) &&
                                (modeSetting.isSelected("Default") || whiteList(s.getStack().getItem())) &&
                                stopWatch.every(delaySetting.getValue())) {
                            InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true);
                        }
                    });
                }
            }
        }
    }

    private void handleCopperDungeon() {
        if (mc.world == null || mc.player == null) return;

        if (barrelScanTimer.every(500)) {
            scanForBarrelsAndText();
        }

        updateTimersFromText();

        lootCurrentBarrel();

        if (openCooldown.every(openDelaySetting.getValue())) {
            openNearbyBarrels();
        }
    }

    private void scanForBarrelsAndText() {
        if (mc.world == null || mc.player == null) return;

        barrelsToLoot.clear();
        barrelTextEntities.clear();

        int range = (int) rangeSetting.getValue();
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    if (mc.world.getBlockState(pos).getBlock().getTranslationKey().toLowerCase().contains("barrel")) {
                        if (!pos.equals(lastOpenedBarrel)) {
                            barrelsToLoot.add(pos);
                        }
                    }
                }
            }
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TextDisplayEntity textEntity) {
                Text displayText = textEntity.getText();
                if (displayText == null) continue;

                String textStr = displayText.getString();
                Matcher matcher = TIME_PATTERN.matcher(textStr);

                if (matcher.find()) {
                    BlockPos textPos = textEntity.getBlockPos().down();

                    if (mc.world.getBlockState(textPos).getBlock().getTranslationKey().toLowerCase().contains("barrel")) {
                        barrelTextEntities.put(textPos, textEntity);
                    }
                }
            }
        }
    }

    private void updateTimersFromText() {
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<BlockPos, TextDisplayEntity> entry : barrelTextEntities.entrySet()) {
            BlockPos barrelPos = entry.getKey();
            TextDisplayEntity textEntity = entry.getValue();

            Text displayText = textEntity.getText();
            if (displayText == null) continue;

            String textStr = displayText.getString();
            Matcher matcher = TIME_PATTERN.matcher(textStr);

            if (matcher.find()) {
                try {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));

                    long totalSeconds = minutes * 60L + seconds;

                    if (totalSeconds == 0) {
                        barrelTimers.put(barrelPos, currentTime);
                    } else {
                        long lootTime = currentTime + (totalSeconds * 1000L);
                        barrelTimers.put(barrelPos, lootTime);
                    }

                } catch (NumberFormatException e) {
                }
            }
        }

        barrelTimers.keySet().removeIf(pos -> !barrelTextEntities.containsKey(pos));
    }

    private void lootCurrentBarrel() {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;

        String title = screen.getTitle().getString().toLowerCase();

        if (title.contains("бочка") || title.contains("barrel")) {
            screen.getScreenHandler().slots.stream()
                    .filter(s -> s.hasStack() && !s.inventory.equals(mc.player.getInventory()))
                    .filter(s -> isValuableItemForDungeon(s.getStack()))
                    .filter(s -> stopWatch.every(50))
                    .forEach(s -> InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true));

            boolean hasValuableItems = screen.getScreenHandler().slots.stream()
                    .anyMatch(s -> s.hasStack() && !s.inventory.equals(mc.player.getInventory()) && isValuableItemForDungeon(s.getStack()));

            if (!hasValuableItems) {
                mc.player.closeHandledScreen();
                lastOpenedBarrel = null;
            }
        }
    }

    private void openNearbyBarrels() {
        if (barrelsToLoot.isEmpty()) return;

        long currentTime = System.currentTimeMillis();

        for (BlockPos barrelPos : barrelsToLoot) {
            double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(barrelPos));

            if (distance <= 3.0) {

                Long lootTime = barrelTimers.get(barrelPos);
                if (lootTime != null) {
                    long timeLeft = lootTime - currentTime;

                    if (timeLeft <= 0) {
                        openBarrel(barrelPos);
                        lastOpenedBarrel = barrelPos;
                        break;
                    }
                } else {
                    openBarrel(barrelPos);
                    lastOpenedBarrel = barrelPos;
                    break;
                }
            }
        }
    }

    private void openBarrel(BlockPos pos) {
        if (mc.player == null || mc.player.networkHandler == null) return;

        try {
            Vec3d hitVec = Vec3d.ofCenter(pos);

            PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(
                    mc.player.getActiveHand(),
                    new BlockHitResult(
                            hitVec,
                            Direction.UP,
                            pos,
                            false
                    ),
                    0
            );

            mc.player.networkHandler.sendPacket(packet);
            sendLookPacket(pos);

        } catch (Exception e) {
            System.err.println("[ChestStealer] Ошибка открытия бочки: " + e.getMessage());
        }
    }

    private void sendLookPacket(BlockPos pos) {
        if (mc.player == null || mc.player.networkHandler == null) return;

        try {
            Vec3d barrelCenter = Vec3d.ofCenter(pos);
            Vec3d playerEyes = mc.player.getEyePos();

            double diffX = barrelCenter.x - playerEyes.x;
            double diffY = barrelCenter.y - playerEyes.y;
            double diffZ = barrelCenter.z - playerEyes.z;

            double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

            float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
            float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distXZ));

            yaw = yaw % 360.0F;
            if (yaw < -180.0F) yaw += 360.0F;
            if (yaw >= 180.0F) yaw -= 360.0F;

            pitch = Math.max(-90.0F, Math.min(90.0F, pitch));

            mc.player.networkHandler.sendPacket(
                    new PlayerMoveC2SPacket.LookAndOnGround(
                            yaw,
                            pitch,
                            mc.player.isOnGround(),
                            false
                    )
            );

        } catch (Exception e) {
            System.err.println("[ChestStealer] Ошибка отправки look пакета: " + e.getMessage());
        }
    }

    private boolean isValuableItemForDungeon(ItemStack stack) {
        Item item = stack.getItem();

        if (item == Items.TRIPWIRE_HOOK) return true;

        if (item == Items.ENDER_EYE) return true;

        if (item == Items.SNOWBALL) return true;

        if (item == Items.SPLASH_POTION) return true;

        if (item == Items.NETHERITE_SCRAP) return true;

        if (item == Items.ENCHANTED_GOLDEN_APPLE) return true;

        if (item == Items.GOLDEN_APPLE) return true;

        if (item == Items.TNT) return true;

        if (item == Items.PLAYER_HEAD) return true;

        if (item == Items.TOTEM_OF_UNDYING) return true;

        if (item == Items.IRON_INGOT) return true;

        if (item == Items.EXPERIENCE_BOTTLE) return true;

        return whiteList(item);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!modeSetting.isSelected("CopperDungeon") || !tracerSetting.isValue() || mc.player == null) return;

        long currentTime = System.currentTimeMillis();

        for (Map.Entry<BlockPos, Long> entry : barrelTimers.entrySet()) {
            BlockPos pos = entry.getKey();
            Long lootTime = entry.getValue();
            if (lootTime == null) continue;

            long timeLeft = lootTime - currentTime;
            if (timeLeft <= tracerTimeSetting.getValue() * 1000L) {
                Vec3d playerPos = mc.player.getEyePos();
                Vec3d barrelPos = Vec3d.ofCenter(pos);

                Render3D.drawLine(playerPos, barrelPos, tracerColor.getColor(), 2, false);

                int color = tracerColor.getColor();
                if (timeLeft <= 0) {
                    color = ColorAssist.getColor(0, 255, 0, 255);
                } else if (timeLeft <= 10000) {
                    color = ColorAssist.getColor(255, 255, 0, 255);
                }

                Render3D.drawBox(new net.minecraft.util.math.Box(pos), color, 1, false, true, false);
            }
        }
    }

    private boolean whiteList(Item item) {
        return itemSettings.getSelected().toString().toLowerCase()
                .contains(item.toString().toLowerCase().replace("_", " "));
    }
}