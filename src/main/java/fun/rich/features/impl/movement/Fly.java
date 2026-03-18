package fun.rich.features.impl.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.events.player.TickEvent;
import fun.rich.utils.client.Instance;
import fun.rich.utils.math.time.StopWatch;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Fly extends Module {
    public static Fly getInstance() {
        return Instance.get(Fly.class);
    }

    SelectSetting mode = new SelectSetting("Режим", "Выберите режим полета")
            .value("Normal", "MX", "Dragon Fly", "CommandBlock")
            .selected("Normal");

    SliderSettings speedXZ = new SliderSettings("Скорость XZ", "Горизонтальная скорость")
            .setValue(1.5F).range(1.0F, 10.0F)
            .visible(() -> !mode.isSelected("CommandBlock"));

    SliderSettings speedY = new SliderSettings("Скорость Y", "Вертикальная скорость")
            .setValue(1.5F).range(0.0F, 10.0F)
            .visible(() -> !mode.isSelected("CommandBlock"));

    SliderSettings mxGlide = new SliderSettings("MX: Глиссер", "Лёгкое падение при отпускании (имитация Slow Falling)")
            .setValue(0.02F).range(0.0F, 0.08F)
            .visible(() -> mode.isSelected("MX"));

    SliderSettings commandSpeed = new SliderSettings("Скорость", "Скорость полета с командными блоками")
            .setValue(0.8F).range(0.1F, 3.0F)
            .visible(() -> mode.isSelected("CommandBlock"));

    SliderSettings placeDelay = new SliderSettings("Задержка", "Задержка между установкой блоков (мс)")
            .setValue(200).range(50, 1000)
            .visible(() -> mode.isSelected("CommandBlock"));

    @NonFinal StopWatch timer = new StopWatch();
    @NonFinal StopWatch placeTimer = new StopWatch();
    @NonFinal int commandBlockSlot = -1;
    @NonFinal int tickCounter = 0;

    public Fly() {
        super("Fly", ModuleCategory.MOVEMENT);
        setup(mode, speedXZ, speedY, mxGlide, commandSpeed, placeDelay);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!state || mc.player == null || mc.world == null) return;

        if (mode.isSelected("Normal")) {
            handleNormalFly();
        } else if (mode.isSelected("MX")) {
            handleMXFly();
        } else if (mode.isSelected("Dragon Fly")) {
            handleDragonFly();
        } else if (mode.isSelected("CommandBlock")) {
            handleCommandBlockFly();
        }
    }

    /**
     * MX bypass: onGround=false, консервативная скорость, имитация Slow Falling.
     * MX не имеет FlyCheck; GhostBlock — только при onGround=true; RawMovement — NaN/|v|>3E8.
     */
    private void handleMXFly() {
        float speed = Math.min(speedXZ.getValue(), 0.8f);
        float vertSpeed = Math.min(speedY.getValue(), 0.6f);
        double motionY = getMotionY();
        if (motionY == 0.0) {
            motionY = -mxGlide.getValue();
        } else {
            motionY = motionY > 0 ? vertSpeed : -vertSpeed;
        }
        setMotion(speed);
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(
                MathHelper.clamp(v.x, -2.0, 2.0),
                MathHelper.clamp(motionY, -1.5, 1.5),
                MathHelper.clamp(v.z, -2.0, 2.0)
        );
        mc.player.setOnGround(false);
        mc.player.fallDistance = 0.0f;
    }

    private void handleNormalFly() {
        double motionY = getMotionY();
        setMotion(speedXZ.getValue());
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x, motionY, v.z);
    }

    private void handleDragonFly() {
        if (mc.player.getAbilities().flying) {
            setMotion(speedXZ.getValue());
            double motionY = 0.0;
            if (mc.options.jumpKey.isPressed()) {
                motionY = speedY.getValue();
            }
            if (mc.options.sneakKey.isPressed()) {
                motionY = -speedY.getValue();
            }
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, motionY, v.z);
        }
    }

    private void handleCommandBlockFly() {
        if (commandBlockSlot == -1) {
            commandBlockSlot = findCommandBlockSlot();
            if (commandBlockSlot == -1) {
                return;
            }
        }
        ItemStack stack = mc.player.getInventory().getStack(commandBlockSlot);
        if (stack.isEmpty() || !isCommandBlock(stack)) {
            commandBlockSlot = -1;
            return;
        }

        mc.player.getInventory().selectedSlot = commandBlockSlot;

        mc.player.setNoGravity(true);

        float speed = commandSpeed.getValue();
        double motionX = 0;
        double motionZ = 0;
        double motionY = 0;

        float yaw = mc.player.getYaw();
        if (mc.options.forwardKey.isPressed()) {
            motionX -= MathHelper.sin(yaw * ((float)Math.PI / 180)) * speed;
            motionZ += MathHelper.cos(yaw * ((float)Math.PI / 180)) * speed;
        }
        if (mc.options.backKey.isPressed()) {
            motionX += MathHelper.sin(yaw * ((float)Math.PI / 180)) * speed;
            motionZ -= MathHelper.cos(yaw * ((float)Math.PI / 180)) * speed;
        }
        if (mc.options.leftKey.isPressed()) {
            motionX -= MathHelper.sin((yaw + 90) * ((float)Math.PI / 180)) * speed;
            motionZ += MathHelper.cos((yaw + 90) * ((float)Math.PI / 180)) * speed;
        }
        if (mc.options.rightKey.isPressed()) {
            motionX -= MathHelper.sin((yaw - 90) * ((float)Math.PI / 180)) * speed;
            motionZ += MathHelper.cos((yaw - 90) * ((float)Math.PI / 180)) * speed;
        }

        if (mc.options.jumpKey.isPressed()) motionY = speed;
        if (mc.options.sneakKey.isPressed()) motionY = -speed;

        mc.player.setVelocity(motionX, motionY, motionZ);

        tickCounter++;
        if (placeTimer.finished((long)placeDelay.getValue()) && tickCounter % 2 == 0) {
            BlockPos footPos = mc.player.getBlockPos().down();
            if (mc.world.getBlockState(footPos).isAir()) {
                sendFakePlacement(footPos, Direction.UP);
            }

            if (tickCounter % 10 == 0) {
                BlockPos[] sidePositions = {
                        footPos.north(),
                        footPos.south(),
                        footPos.east(),
                        footPos.west()
                };

                for (BlockPos sidePos : sidePositions) {
                    if (mc.world.getBlockState(sidePos).isAir()) {
                        sendFakePlacement(sidePos, Direction.UP);
                        break;
                    }
                }
            }

            placeTimer.reset();
        }

        mc.player.setOnGround(true);
        mc.player.fallDistance = 0.0f;
    }

    private int findCommandBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.REPEATING_COMMAND_BLOCK) {
                return i;
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && isCommandBlock(stack)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isCommandBlock(ItemStack stack) {
        return stack.getItem() == Items.COMMAND_BLOCK ||
                stack.getItem() == Items.CHAIN_COMMAND_BLOCK ||
                stack.getItem() == Items.REPEATING_COMMAND_BLOCK;
    }

    private void sendFakePlacement(BlockPos pos, Direction side) {
        Vec3d hitPos = new Vec3d(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );

        BlockHitResult hitResult = new BlockHitResult(
                hitPos,
                side,
                pos,
                false
        );

        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND,
                hitResult,
                0
        );

        mc.player.networkHandler.sendPacket(packet);
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private double getMotionY() {
        if (mc.options.sneakKey.isPressed()) {
            return -speedY.getValue();
        } else if (mc.options.jumpKey.isPressed()) {
            return speedY.getValue();
        }
        return 0.0;
    }

    private void setMotion(float speed) {
        float yaw = mc.player.getYaw();
        float f = mc.player.forwardSpeed;
        float s = mc.player.sidewaysSpeed;
        float speedScale = speed / 3.0F;
        double x = 0.0;
        double z = 0.0;
        if (f != 0.0F || s != 0.0F) {
            float yawRad = yaw * ((float)Math.PI / 180.0F);
            x = -MathHelper.sin(yawRad) * speedScale * f + MathHelper.cos(yawRad) * speedScale * s;
            z = MathHelper.cos(yawRad) * speedScale * f + MathHelper.sin(yawRad) * speedScale * s;
        }
        mc.player.setVelocity(x, mc.player.getVelocity().y, z);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        timer.reset();

        if (mode.isSelected("CommandBlock")) {
            mc.player.setNoGravity(false);
            commandBlockSlot = -1;
            tickCounter = 0;
        }
    }
}