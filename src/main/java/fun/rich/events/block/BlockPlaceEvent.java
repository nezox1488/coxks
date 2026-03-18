package fun.rich.events.block;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import fun.rich.utils.client.managers.event.events.Event;

public record BlockPlaceEvent(BlockPos pos, Block block) implements Event {}