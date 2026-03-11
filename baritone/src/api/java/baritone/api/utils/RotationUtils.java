package baritone.api.utils;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.Random;

public final class RotationUtils {
    public static final double DEG_TO_RAD = Math.PI / 180.0;
    public static final float DEG_TO_RAD_F = (float) DEG_TO_RAD;
    public static final double RAD_TO_DEG = 180.0 / Math.PI;
    public static final float RAD_TO_DEG_F = (float) RAD_TO_DEG;

    private static final Random RND = new Random();

    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(0.5, 0.1, 0.5), // Down
            new Vec3(0.5, 0.9, 0.5), // Up
            new Vec3(0.5, 0.5, 0.1), // North
            new Vec3(0.5, 0.5, 0.9), // South
            new Vec3(0.1, 0.5, 0.5), // West
            new Vec3(0.9, 0.5, 0.5)  // East
    };

    private RotationUtils() {}

    private static float getLegitNoise() {
        return (RND.nextFloat() - 0.5f) * 0.0002f;
    }

    public static Rotation calcRotationFromCoords(BlockPos orig, BlockPos dest) {
        return calcRotationFromVec3d(new Vec3(orig.getX(), orig.getY(), orig.getZ()), new Vec3(dest.getX(), dest.getY(), dest.getZ()));
    }

    public static Rotation wrapAnglesToRelative(Rotation current, Rotation target) {
        if (current.yawIsReallyClose(target)) {
            return new Rotation(current.getYaw(), target.getPitch() + getLegitNoise());
        }
        return target.subtract(current).normalize().add(current);
    }

    public static Rotation calcRotationFromVec3d(Vec3 orig, Vec3 dest, Rotation current) {
        return wrapAnglesToRelative(current, calcRotationFromVec3d(orig, dest));
    }

    private static Rotation calcRotationFromVec3d(Vec3 orig, Vec3 dest) {
        double[] delta = {orig.x - dest.x, orig.y - dest.y, orig.z - dest.z};
        double yaw = Mth.atan2(delta[0], -delta[2]);
        double dist = Math.sqrt(delta[0] * delta[0] + delta[2] * delta[2]);
        double pitch = Mth.atan2(delta[1], dist);

        return new Rotation(
                (float) (yaw * RAD_TO_DEG) + getLegitNoise(),
                (float) (pitch * RAD_TO_DEG) + getLegitNoise()
        );
    }

    public static Vec3 calcLookDirectionFromRotation(Rotation rotation) {
        float flatZ = Mth.cos((-rotation.getYaw() * DEG_TO_RAD_F) - (float) Math.PI);
        float flatX = Mth.sin((-rotation.getYaw() * DEG_TO_RAD_F) - (float) Math.PI);
        float pitchBase = -Mth.cos(-rotation.getPitch() * DEG_TO_RAD_F);
        float pitchHeight = Mth.sin(-rotation.getPitch() * DEG_TO_RAD_F);
        return new Vec3(flatX * pitchBase, pitchHeight, flatZ * pitchBase);
    }

    public static Optional<Rotation> reachable(IPlayerContext ctx, BlockPos pos, double blockReachDistance, boolean wouldSneak) {
        BlockState state = ctx.world().getBlockState(pos);
        VoxelShape shape = state.getShape(ctx.world(), pos);
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }

        double xRand = shape.min(Direction.Axis.X) + (shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X)) * (0.3 + RND.nextDouble() * 0.4);
        double yRand = shape.min(Direction.Axis.Y) + (shape.max(Direction.Axis.Y) - shape.min(Direction.Axis.Y)) * (0.3 + RND.nextDouble() * 0.4);
        double zRand = shape.min(Direction.Axis.Z) + (shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z)) * (0.3 + RND.nextDouble() * 0.4);

        Vec3 randomSurfacePoint = new Vec3(pos.getX() + xRand, pos.getY() + yRand, pos.getZ() + zRand);
        Optional<Rotation> randomRot = reachableOffset(ctx, pos, randomSurfacePoint, blockReachDistance, wouldSneak);
        if (randomRot.isPresent()) {
            return randomRot;
        }

        for (Vec3 sideOffset : BLOCK_SIDE_MULTIPLIERS) {
            double xShift = (RND.nextDouble() - 0.5) * 0.1;
            double yShift = (RND.nextDouble() - 0.5) * 0.1;
            double zShift = (RND.nextDouble() - 0.5) * 0.1;

            double xDiff = shape.min(Direction.Axis.X) * sideOffset.x + shape.max(Direction.Axis.X) * (1 - sideOffset.x) + xShift;
            double yDiff = shape.min(Direction.Axis.Y) * sideOffset.y + shape.max(Direction.Axis.Y) * (1 - sideOffset.y) + yShift;
            double zDiff = shape.min(Direction.Axis.Z) * sideOffset.z + shape.max(Direction.Axis.Z) * (1 - sideOffset.z) + zShift;

            Optional<Rotation> possibleRotation = reachableOffset(ctx, pos, new Vec3(pos.getX(), pos.getY(), pos.getZ()).add(xDiff, yDiff, zDiff), blockReachDistance, wouldSneak);
            if (possibleRotation.isPresent()) {
                return possibleRotation;
            }
        }
        return Optional.empty();
    }

    public static Optional<Rotation> reachableOffset(IPlayerContext ctx, BlockPos pos, Vec3 offsetPos, double blockReachDistance, boolean wouldSneak) {
        Vec3 eyes = wouldSneak ? RayTraceUtils.inferSneakingEyePosition(ctx.player()) : ctx.player().getEyePosition(1.0F);

        Rotation rotation = calcRotationFromVec3d(eyes, offsetPos, ctx.playerRotations());

        Rotation actualRotation = BaritoneAPI.getProvider().getBaritoneForPlayer(ctx.player()).getLookBehavior().getAimProcessor().peekRotation(rotation);
        HitResult result = RayTraceUtils.rayTraceTowards(ctx.player(), actualRotation, blockReachDistance, wouldSneak);

        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) result;
            if (bhr.getBlockPos().equals(pos)) {
                return Optional.of(rotation);
            }
            if (ctx.world().getBlockState(pos).getBlock() instanceof BaseFireBlock && bhr.getBlockPos().equals(pos.below())) {
                return Optional.of(rotation);
            }
        }
        return Optional.empty();
    }

    public static Optional<Rotation> reachableCenter(IPlayerContext ctx, BlockPos pos, double blockReachDistance, boolean wouldSneak) {
        return reachable(ctx, pos, blockReachDistance, wouldSneak);
    }
    @Deprecated
    public static Optional<Rotation> reachable(LocalPlayer entity, BlockPos pos, double blockReachDistance, boolean wouldSneak) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(entity);
        return reachable(baritone.getPlayerContext(), pos, blockReachDistance, wouldSneak);
    }
}
