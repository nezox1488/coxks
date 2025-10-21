package fun.rich.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.client.managers.event.types.EventType;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.events.player.PostTickEvent;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.interactions.simulate.PlayerSimulation;
import fun.rich.utils.math.time.StopWatch;
import fun.rich.utils.math.task.TaskPriority;
import fun.rich.utils.math.script.Script;
import fun.rich.events.player.RotationUpdateEvent;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.warp.TurnsConfig;
import fun.rich.utils.features.aura.warp.TurnsConnection;
import fun.rich.utils.features.aura.rotations.impl.SnapAngle;

import java.util.stream.Stream;

@Setter
@Getter
@SuppressWarnings("unused")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Spider extends Module {
    Script script = new Script();
    StopWatch stopWatch = new StopWatch();

    SelectSetting mode = new SelectSetting("Режим", "Выбирает режим")
            .value("Block", "SpookyTime", "FunTime").selected("Block");

    public Spider() {
        super("Spider", ModuleCategory.MOVEMENT);
        setup(mode);
    }
    private int useItemSequence = 0;
    private double lastWaterY = 0;
    private long lastWaterPlaceTime = 0;


    @EventHandler
    public void onPostTick(PostTickEvent e) {

        if (stopWatch.finished(120)) {
            mc.player.setVelocity(0, 0.4F, 0);
            mc.player.setOnGround(true);
            stopWatch.reset();
        }

        if (mode.isSelected("FunTime")) {
            if (mc.options.jumpKey.isPressed()) return;
            Box playerBox = mc.player.getBoundingBox().expand(-1e-3);
            Box box = new Box(playerBox.minX, playerBox.minY, playerBox.minZ, playerBox.maxX, playerBox.minY + 0.5, playerBox.maxZ);
            if (stopWatch.finished(400) && PlayerInteractionHelper.isBox(box, this::hasCollision)) {
                box = new Box(playerBox.minX - 0.3, playerBox.minY + 1, playerBox.minZ - 0.3, playerBox.maxX, playerBox.maxY, playerBox.maxZ);
                if (PlayerInteractionHelper.isBox(box, this::hasCollision)) {
                    mc.player.setOnGround(true);
                    mc.player.velocity.y = 0.6;
                } else {
                    mc.player.setOnGround(true);
                    mc.player.jump();
                }
            }
        }

        if (mode.isSelected("SpookyTime") || mode.isSelected("FixedPitch")) {

            if (mc.player.horizontalCollision) {
                mc.options.forwardKey.setPressed(false);
            }

            int waterBucketSlot = InventoryTask.getHotbarSlotId(i -> mc.player.getInventory().getStack(i).getItem() == Items.WATER_BUCKET);
            boolean hasWaterBucketInMain = mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET;

            if (waterBucketSlot >= 0 && waterBucketSlot <= 8) {
                mc.options.jumpKey.setPressed(true);
                mc.options.sneakKey.setPressed(true);
                mc.player.getInventory().setSelectedSlot(waterBucketSlot);
            } else {
                ChatMessage.brandmessage("Нужно ведро воды в хотбаре");
            }

            if (hasWaterBucketInMain) {
                BlockPos placeablePos = getPlaceableWaterBlock();
                if (placeablePos != null && stopWatch.finished(340)) {
                    mc.player.setPitch(75);
                    Vec3d vec = placeablePos.toCenterPos();
                    Direction direction = Direction.getFacing(vec.x - mc.player.getX(), vec.y - mc.player.getY(), vec.z - mc.player.getZ());
                    mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 1, mc.player.getYaw(), mc.player.getPitch()));
                    mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    stopWatch.reset();
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.35, mc.player.getVelocity().z);
                }
            }
        }
    }


    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.PRE) {
            if (mode.isSelected("Block")) {
                boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
                int slotId = InventoryTask.getHotbarSlotId(i -> mc.player.getInventory().getStack(i).getItem() instanceof BlockItem);
                BlockPos blockPos = findPos();
                if (script.isFinished() && (offHand || slotId != -1) && !blockPos.equals(BlockPos.ORIGIN)) {
                    ItemStack stack = offHand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slotId);
                    Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    Vec3d vec = blockPos.toCenterPos();
                    Direction direction = Direction.getFacing(vec.x - mc.player.getX(), vec.y - mc.player.getY(), vec.z - mc.player.getZ());
                    Turns angle = MathAngle.calculateAngle(vec.subtract(new Vec3d(direction.getVector()).multiply(0.1F)));
                    Turns.VecRotation vecRotation = new Turns.VecRotation(angle, angle.toVector());
                    TurnsConnection.INSTANCE.rotateTo(vecRotation, mc.player, 1, new TurnsConfig(new SnapAngle(), true, true), TaskPriority.HIGH_IMPORTANCE_1, this);
                    if (canPlace(stack)) {
                        int prev = mc.player.inventory.selectedSlot;
                        if (!offHand) mc.player.inventory.selectedSlot = slotId;
                        mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(vec, direction.getOpposite(), blockPos, false));
                        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                        if (!offHand) mc.player.inventory.selectedSlot = prev;
                    }
                }
            }
        }
    }


    private boolean canPlace(ItemStack stack) {
        BlockPos blockPos = getBlockPos();
        if (blockPos.getY() >= mc.player.getBlockY()) return false;
        BlockItem blockItem = (BlockItem) stack.getItem();
        VoxelShape shape = blockItem.getBlock().getDefaultState().getCollisionShape(mc.world, blockPos);
        if (shape.isEmpty()) return false;
        Box box = shape.getBoundingBox().offset(blockPos);
        return !box.intersects(mc.player.getBoundingBox()) && box.intersects(PlayerSimulation.simulateLocalPlayer(4).boundingBox);
    }


    private BlockPos findPos() {
        BlockPos blockPos = getBlockPos();
        if (mc.world.getBlockState(blockPos).isSolid()) return BlockPos.ORIGIN;
        return Stream.of(blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north()).filter(pos -> mc.world.getBlockState(pos).isSolid()).findFirst().orElse(BlockPos.ORIGIN);
    }

    private BlockPos getPlaceableWaterBlock() {
        BlockPos below = BlockPos.ofFloored(mc.player.getPos().add(0, -1.3, 0));
        if (mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
            return below;
        }

        for (Direction dir : Direction.values()) {
            BlockPos side = below.offset(dir);
            if (mc.world.getBlockState(side).isSolidBlock(mc.world, side)) {
                return side;
            }
        }

        return null;
    }


    private BlockPos getBlockPos() {
        return BlockPos.ofFloored(PlayerSimulation.simulateLocalPlayer(1).pos.add(0, -1e-3, 0));
    }
    private boolean hasCollision(BlockPos blockPos) {
        return !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
    }
}
