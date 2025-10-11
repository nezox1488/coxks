package fun.rich.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.MultiSelectSetting;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.utils.display.geometry.Render3D;
import fun.rich.events.block.BlockUpdateEvent;
import fun.rich.events.render.WorldLoadEvent;
import fun.rich.events.render.WorldRenderEvent;

import java.util.HashMap;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XRay extends Module {
    Map<BlockPos, BlockState> map = new HashMap<>();

    SelectSetting modeSetting = new SelectSetting("Режим", "Режим работы для XRay").value("Block Update");
    MultiSelectSetting blockTypeSetting = new MultiSelectSetting("Блоки", "Блоки, которые будут отображаться")
            .value("Ancient Debris", "Diamond", "Emerald", "Iron", "Gold");

    public XRay() {
        super("XRay", ModuleCategory.RENDER);
        setup(modeSetting, blockTypeSetting);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        map.forEach((key, value) -> {
            if (blockTypeSetting.getSelected().toString().toLowerCase().contains(getBlockName(value))) {
                Render3D.drawBox(new Box(key), getColorByBlock(value), 1);
            }
        });
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        map.clear();
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent e) {
        BlockState state = e.state();
        BlockPos pos = e.pos();
        switch (e.type()) {
            case BlockUpdateEvent.Type.UPDATE -> {
                if (getColorByBlock(state) != -1 && !map.containsKey(pos)) map.put(pos, state);
                if (map.containsKey(pos) && !map.get(pos).equals(state)) map.remove(pos);
            }
            case BlockUpdateEvent.Type.UNLOAD -> map.remove(pos);
        }
    }
    
    private int getColorByBlock(BlockState block) {
        return switch (getBlockName(block)) {
            case "ancient debris" -> 0xFFA67554;
            case "diamond" -> 0xFF197B81;
            case "emerald" -> 0xFF41871B;
            case "iron" -> 0xFF754C1F;
            case "gold" -> 0xFFC5B938;
            default -> -1;
        };
    }

    private String getBlockName(BlockState state) {
        return state.getBlock().asItem().toString().replace("minecraft:", "").replace("_ore", "").replace("_", " ");
    }
}
