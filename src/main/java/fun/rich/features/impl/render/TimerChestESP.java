package fun.rich.features.impl.render;
/**
 * @author Sitoku
 * @since 3/3/2026
 */
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.geometry.Render3D;
import fun.rich.events.render.WorldRenderEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TimerChestESP extends Module {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})");
    private static final int COLOR_RED = ColorAssist.getColor(255, 60, 60, 200);
    private static final int COLOR_YELLOW = ColorAssist.getColor(255, 220, 60, 200);
    private static final int COLOR_GREEN = ColorAssist.getColor(60, 255, 80, 200);

    SliderSettings range = new SliderSettings("Радиус", "Радиус поиска").setValue(48).range(8, 128);
    BooleanSetting drawTracer = new BooleanSetting("Трассер", "Линия от прицела к сундуку цветом таймера").setValue(true);
    SliderSettings tracerWidth = new SliderSettings("Толщина трассера", "Толщина линии").setValue(2f).range(1f, 4f).visible(() -> drawTracer.isValue());

    Map<BlockPos, Long> chestTimers = new HashMap<>();
    Map<BlockPos, Integer> chestColors = new HashMap<>();
    long lastScanTime = 0;

    public TimerChestESP() {
        super("TimerChestESP", "Timer Chest ESP", ModuleCategory.RENDER);
        setup(range, drawTracer, tracerWidth);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        chestTimers.clear();
        chestColors.clear();
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent e) {
        if (!state || mc.world == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        if (now - lastScanTime >= 300) {
            scanChestsWithTimers();
            lastScanTime = now;
        }

        updateColors(now);

        Vec3d tracerStart = getTracerStartPos();
        float width = tracerWidth.getValue();

        chestColors.forEach((pos, color) -> {
            Box box = new Box(pos);
            Render3D.drawBox(box, color, 1);

            if (drawTracer.isValue()) {
                Vec3d chestCenter = Vec3d.ofCenter(pos);
                Render3D.drawLine(tracerStart, chestCenter, color, width, true);
            }
        });
    }

    private Vec3d getTracerStartPos() {
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d cameraPos = mc.player.getCameraPosVec(tickDelta);
        Vec3d lookVec = mc.player.getRotationVec(tickDelta);
        return cameraPos.add(lookVec.multiply(2.0));
    }

    private void scanChestsWithTimers() {
        Map<BlockPos, Long> newTimers = new HashMap<>();
        BlockPos playerPos = mc.player.getBlockPos();
        int r = (int) range.getValue();

        for (Entity entity : mc.world.getEntities()) {
            String timeStr = null;

            if (entity instanceof TextDisplayEntity textEntity) {
                Text displayText = textEntity.getText();
                if (displayText != null) timeStr = displayText.getString();
            } else if (entity instanceof ArmorStandEntity armorStand && armorStand.hasCustomName()) {
                Text name = armorStand.getCustomName();
                if (name != null) timeStr = name.getString();
            }

            if (timeStr == null) continue;

            Matcher matcher = TIME_PATTERN.matcher(timeStr.replaceAll("§.", ""));
            if (!matcher.find()) continue;

            BlockPos blockPos = entity.getBlockPos().down();
            if (playerPos.getSquaredDistance(blockPos) > r * r) continue;

            Block block = mc.world.getBlockState(blockPos).getBlock();
            String key = Registries.BLOCK.getId(block).toString();
            if (!key.contains("chest") && !key.contains("barrel") && !key.contains("shulker")) continue;

            try {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                long totalSeconds = minutes * 60L + seconds;
                long lootTime = System.currentTimeMillis() + totalSeconds * 1000L;
                newTimers.put(blockPos.toImmutable(), lootTime);
            } catch (NumberFormatException ignored) {
            }
        }

        chestTimers = newTimers;
    }

    private boolean isChestBlock(BlockPos pos) {
        if (!mc.world.isChunkLoaded(pos)) return false;
        String key = mc.world.getBlockState(pos).getBlock().getTranslationKey().toLowerCase();
        return key.contains("chest") || key.contains("barrel") || key.contains("shulker");
    }

    private void updateColors(long currentTime) {
        chestColors.clear();

        chestTimers.forEach((pos, lootTime) -> {
            if (!isChestBlock(pos)) return;
            long timeLeftMs = lootTime - currentTime;
            long timeLeftSec = timeLeftMs / 1000;

            int color;
            if (timeLeftSec <= 30) {
                color = COLOR_GREEN;
            } else if (timeLeftSec < 120) {
                color = COLOR_YELLOW;
            } else {
                color = COLOR_RED;
            }
            chestColors.put(pos, color);
        });
    }
}
