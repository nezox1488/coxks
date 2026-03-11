package fun.rich.features.impl.movement;
// coded by crashgun
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.events.player.MotionEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Spider extends Module {

    private long lastBucketUse = 0L;
    private boolean hasWallContact = false;

    public Spider() {
        super("Spider", ModuleCategory.MOVEMENT);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        mc.options.sneakKey.setPressed(false);
    }

    @EventHandler
    private void onMotion(MotionEvent e) {
        checkWallContact();
        handleBucketUsage();

        if (!hasWallContact) {
            mc.options.sneakKey.setPressed(false);
        }
    }

    private void checkWallContact() {
        hasWallContact = mc.player.horizontalCollision;
    }

    private void handleBucketUsage() {
        int bucketSlot = locateWaterBucket();
        if (bucketSlot == -1) return;

        if (hasWallContact) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
            if (mc.player.getVelocity().y <= 0.05) {
                performWallBoost(bucketSlot);
            }
        }
    }

    private int locateWaterBucket() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }

    private void performWallBoost(int bucketSlot) {
        mc.options.sneakKey.setPressed(true);

        mc.player.setPitch(75.0F);

        if (System.currentTimeMillis() - lastBucketUse < getCooldownByHeight())
            return;

        swapAndUseBucket(bucketSlot);
        lastBucketUse = System.currentTimeMillis();
    }

    private void swapAndUseBucket(int slot) {
        int prevSlot = mc.player.getInventory().selectedSlot;
        if (slot != prevSlot)
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        mc.player.setPitch(-90F);
        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));

        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x, 0.45, vel.z);

        if (slot != prevSlot)
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
    }

    private long getCooldownByHeight() {
        double height = computeHeightGap();
        if (height < 5) return 450L;
        if (height < 20) return 550L;
        return 650L;
    }

    private double computeHeightGap() {
        if (mc.player == null || mc.world == null) return 0.0;
        double startY = mc.player.getY();
        for (double y = startY; y > 0.0; y -= 0.1) {
            BlockPos pos = BlockPos.ofFloored(mc.player.getX(), y, mc.player.getZ());
            if (!mc.world.getBlockState(pos).isAir())
                return Math.max(startY - (y + 1), 0.0);
        }
        return 0.0;
    }
}
